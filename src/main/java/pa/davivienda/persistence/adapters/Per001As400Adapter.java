package pa.davivienda.persistence.adapters;

import com.ibm.as400.access.AS400;
import pa.davivienda.transversal.constants.TransactionConstants;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;
import pa.davivienda.domain.ports.output.Per001ServicePort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Adapter para invocar el programa RPG PER001 en AS/400.
 * Implementa el port Per001ServicePort utilizando la biblioteca jt400.
 * 
 * <p>El programa PER001 se encarga del cobro de membresías (COBPER) y retorna:
 * - Número de comprobante
 * - Secuencial de operación
 * - Fecha/hora del movimiento
 * - Códigos de respuesta
 * 
 * <p>La configuración de conexión se inyecta desde application.yml mediante
 * MicroProfile Config, permitiendo override con variables de entorno.
 * 
 * <p>Implementa patrones de resiliencia:
 * - Circuit Breaker: Protege contra fallos del AS/400
 * - Retry: Reintentos automáticos con backoff
 * - Timeout: Límite de tiempo de ejecución
 * - Fallback: Respuesta alternativa cuando el servicio no está disponible
 */
@ApplicationScoped
public class Per001As400Adapter implements Per001ServicePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(Per001As400Adapter.class);

    @ConfigProperty(name = "as400.host")
    String as400Host;

    @ConfigProperty(name = "as400.username")
    String as400Username;

    @ConfigProperty(name = "as400.password")
    String as400Password;

    @ConfigProperty(name = "as400.library")
    String library;

    @ConfigProperty(name = "as400.program.per001")
    String programName;

    /**
     * Procesa un cobro de membresía invocando el programa RPG PER001 en AS/400.
     * 
     * <p>Patrones de resiliencia aplicados:
     * - Timeout de 10 segundos para evitar esperas infinitas
     * - 3 reintentos con 1 segundo de delay entre intentos
     * - Circuit Breaker que abre después de 5 fallos consecutivos (50% failure ratio)
     * - Fallback a respuesta de error cuando el circuito está abierto
     * 
     * @param command Comando con los datos de la transferencia/cobro
     * @return Resultado de la operación procesada por PER001
     * @throws RuntimeException Si hay error de conexión o ejecución del programa después de reintentos
     */
    @Override
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 1000, jitter = 200)
    @CircuitBreaker(
        requestVolumeThreshold = 10,
        failureRatio = 0.5,
        delay = 30000,
        successThreshold = 2
    )
    @Fallback(fallbackMethod = "fallbackMembershipPayment")
    public TransferResult processMembershipPayment(TransferCommand command) {
        LOGGER.info("Iniciando llamada a PER001 - idTransaccion={}", command.getIdTransaccion());

        AS400 as400 = null;
        try {
            // Conexión al AS/400
            as400 = new AS400(as400Host, as400Username, as400Password);
            LOGGER.debug("Conectado a AS/400: {}", as400Host);

            // Construir ruta completa del programa
            String qualifiedProgram = "/QSYS.LIB/" + library + ".LIB/" + programName + ".PGM";
            LOGGER.debug("Programa a ejecutar: {}", qualifiedProgram);

            // Preparar parámetros de entrada para PER001
            ProgramParameter[] parameters = buildPer001Parameters(as400, command);

            // Ejecutar programa
            ProgramCall program = new ProgramCall(as400, qualifiedProgram, parameters);
            boolean success = program.run();

            // Procesar resultado
            if (success) {
                TransferResult result = parseSuccessResponse(parameters, command);
                LOGGER.info("PER001 ejecutado exitosamente - comprobante={}", result.getValNumeroComprobante());
                return result;
            } else {
                // Procesar errores del AS/400
                String errorMessage = getAs400ErrorMessage(program);
                LOGGER.error("Error ejecutando PER001: {}", errorMessage);
                throw new RuntimeException("Error en PER001: " + errorMessage);
            }

        } catch (Exception e) {
            LOGGER.error("Error invocando PER001", e);
            throw new RuntimeException("Error al invocar servicio AS/400 PER001", e);
        } finally {
            if (as400 != null) {
                as400.disconnectAllServices();
                LOGGER.debug("Desconectado de AS/400");
            }
        }
    }

    /**
     * Construye los parámetros de entrada/salida para el programa PER001.
     * 
     * Estructura aproximada (ajustar según especificación RPG real):
     * - P1 (IN): Tipo identificación (10 chars)
     * - P2 (IN): Número identificación (20 chars)
     * - P3 (IN): Tipo producto (10 chars)
     * - P4 (IN): Número producto (20 chars)
     * - P5 (IN): Monto (15 packed decimal)
     * - P6 (IN): Moneda (3 chars)
     * - P7 (IN): Concepto (10 chars)
     * - P8 (OUT): Comprobante (20 chars)
     * - P9 (OUT): Secuencial (10 chars)
     * - P10 (OUT): Código respuesta (5 chars)
     * - P11 (OUT): Mensaje respuesta (100 chars)
     */
    private ProgramParameter[] buildPer001Parameters(AS400 as400, TransferCommand command) {
        // Convertidores de texto AS/400 (EBCDIC)
        AS400Text text10 = new AS400Text(10, as400.getCcsid());
        AS400Text text20 = new AS400Text(20, as400.getCcsid());
        AS400Text text3 = new AS400Text(3, as400.getCcsid());
        AS400Text text5 = new AS400Text(5, as400.getCcsid());
        AS400Text text100 = new AS400Text(100, as400.getCcsid());

        // Parámetros de entrada
        byte[] p1TipoIdent = text10.toBytes(nvl(command.getCodTipoIdentificacion(), ""));
        byte[] p2NumIdent = text20.toBytes(nvl(command.getValNumeroIdentificacion(), ""));
        byte[] p3TipoProd = text10.toBytes(nvl(command.getCodTipoProducto(), ""));
        byte[] p4NumProd = text20.toBytes(nvl(command.getValNumeroProducto(), ""));
        byte[] p5Monto = formatAmount(command.getValMonto());
        byte[] p6Moneda = text3.toBytes(nvl(command.getCodMonedaProducto(), "USD"));
        byte[] p7Concepto = text10.toBytes(nvl(command.getCodTipoConcepto(), TransactionConstants.ConceptType.COBPER));

        // Parámetros de salida (inicializados vacíos)
        byte[] p8Comprobante = new byte[20];
        byte[] p9Secuencial = new byte[10];
        byte[] p10CodRespuesta = new byte[5];
        byte[] p11MsgRespuesta = new byte[100];

        return new ProgramParameter[]{
                new ProgramParameter(p1TipoIdent),              // IN
                new ProgramParameter(p2NumIdent),               // IN
                new ProgramParameter(p3TipoProd),               // IN
                new ProgramParameter(p4NumProd),                // IN
                new ProgramParameter(p5Monto),                  // IN
                new ProgramParameter(p6Moneda),                 // IN
                new ProgramParameter(p7Concepto),               // IN
                new ProgramParameter(p8Comprobante),            // OUT
                new ProgramParameter(p9Secuencial),             // OUT
                new ProgramParameter(p10CodRespuesta),          // OUT
                new ProgramParameter(p11MsgRespuesta)           // OUT
        };
    }

    /**
     * Parsea la respuesta exitosa del programa PER001.
     */
    private TransferResult parseSuccessResponse(ProgramParameter[] parameters, TransferCommand command) {
        AS400Text text20 = new AS400Text(20, 37); // CCSID 37 = EBCDIC US/Canada
        AS400Text text10 = new AS400Text(10, 37);
        AS400Text text5 = new AS400Text(5, 37);
        AS400Text text100 = new AS400Text(100, 37);

        // Extraer parámetros de salida
        String comprobante = ((String) text20.toObject(parameters[7].getOutputData())).trim();
        String secuencial = ((String) text10.toObject(parameters[8].getOutputData())).trim();
        String codRespuesta = ((String) text5.toObject(parameters[9].getOutputData())).trim();
        String msgRespuesta = ((String) text100.toObject(parameters[10].getOutputData())).trim();

        LOGGER.debug("Respuesta PER001 - comprobante={}, secuencial={}, cod={}, msg={}",
                comprobante, secuencial, codRespuesta, msgRespuesta);

        // Construir resultado
        TransferResult result = new TransferResult();

        // DataHeader
        result.setNombreOperacion(command.getNombreOperacion());
        result.setTotal(command.getTotal());
        result.setCaracterAceptacion("S"); // Éxito
        result.setUltimoMensaje((short) 1);
        result.setIdTransaccion(command.getIdTransaccion());
        result.setCodMsgRespuesta(parseInteger(codRespuesta, 0));
        result.setMsgRespuesta(msgRespuesta);

        // Data
        result.setValNumeroComprobante(comprobante);
        result.setValSecuencial(parseLong(secuencial, 0L));
        result.setFecHoraMovimiento(OffsetDateTime.now()); // Timestamp actual
        result.setValMonto(command.getValMonto());
        result.setCostoDeLaTransaccion(BigDecimal.ZERO); // PER001 no retorna costos
        result.setValTasaCambio(command.getValTasaCambio());
        result.setValMontoDestino(command.getValMontoDestino());
        result.setCodMonedaTransaccion(command.getCodMonedaProducto());

        return result;
    }

    /**
     * Obtiene el mensaje de error del AS/400.
     */
    private String getAs400ErrorMessage(ProgramCall program) {
        AS400Message[] messageList = program.getMessageList();
        if (messageList.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (AS400Message msg : messageList) {
                sb.append(msg.getID()).append(": ").append(msg.getText()).append("; ");
            }
            return sb.toString();
        }
        return "Error desconocido en AS/400";
    }

    /**
     * Formatea un monto a formato packed decimal (15 bytes).
     * Implementación simplificada - ajustar según especificación RPG.
     */
    private byte[] formatAmount(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        // Convertir a string con 2 decimales
        String amountStr = amount.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toString()
                .replace(".", "");
        
        // Pad con ceros a la izquierda (13 dígitos + signo)
        amountStr = String.format("%013d", Long.parseLong(amountStr));
        
        // Convertir a bytes ASCII (simplificado)
        AS400Text text15 = new AS400Text(15, 37);
        return text15.toBytes(amountStr);
    }

    /**
     * Función auxiliar para manejar nulls.
     */
    private String nvl(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Parsea un string a Integer de forma segura.
     */
    private Integer parseInteger(String value, Integer defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parsea un string a Long de forma segura.
     */
    private Long parseLong(String value, Long defaultValue) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Método fallback que se ejecuta cuando el Circuit Breaker está abierto
     * o cuando todos los reintentos han fallado.
     * 
     * <p>Retorna una respuesta de error controlada indicando que el servicio
     * AS/400 no está disponible temporalmente.
     * 
     * @param command Comando original de la transferencia
     * @return TransferResult con indicación de servicio no disponible
     */
    public TransferResult fallbackMembershipPayment(TransferCommand command) {
        LOGGER.warn("Circuit Breaker ABIERTO o reintentos agotados - ejecutando fallback para idTransaccion={}", 
                   command.getIdTransaccion());
        
        TransferResult result = new TransferResult();
        result.setIdTransaccion(command.getIdTransaccion());
        result.setNombreOperacion("OrqCompensacion");
        result.setCaracterAceptacion("E"); // Error
        result.setCodMsgRespuesta(503); // Service Unavailable
        result.setMsgRespuesta("Servicio AS/400 temporalmente no disponible. Circuit Breaker activado. Intente más tarde.");
        result.setValNumeroComprobante("FB-" + System.currentTimeMillis()); // Fallback ID
        result.setValSecuencial(0L);
        result.setFecHoraMovimiento(OffsetDateTime.now());
        
        return result;
    }
}
