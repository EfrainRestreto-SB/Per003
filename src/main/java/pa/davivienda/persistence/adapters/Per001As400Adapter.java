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
 * Adapter para invocar el CL PER001P en AS/400.
 * Implementa el port Per001ServicePort utilizando la biblioteca jt400.
 * 
 * <p>El CL PER001P recibe dos parámetros:
 * - pinHeader (226 chars): Información de contexto y sesión
 * - pinBody (50 chars): Datos de la operación (identificación y producto)
 * 
 * <p>El CL ejecuta el programa RPG PER001 y retorna el resultado.
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
     * Construye los parámetros de entrada/salida para el CL PER001P.
     * 
     * Estructura de parámetros:
     * - P1 (IN/OUT): pinHeader (264 bytes) - OUTHEADER: ACEPTABM(1) + ERROR(8) + MENSAJER(255)
     * - P2 (IN/OUT): pinBody (54 bytes) - OUTBODY: ONUMCOM(10) + OMONDEB(15) + OMONEDA(4) + OFECHOR(25)
     * 
     * Entrada pinHeader (264 bytes):
     * - servicio (15 chars)
     * - idTransaccion (50 chars)
     * - idSesion (50 chars)
     * - fechaHora (26 chars)
     * - canalAtencion (2 chars)
     * - pais (2 chars)
     * - usuario (10 chars)
     * - idioma (2 chars)
     * - ip (15 chars)
     * - filler (92 chars) para completar 264 bytes
     * 
     * Entrada pinBody (54 bytes):
     * - tipoIdentificacion (2 chars)
     * - numeroIdentificacion (12 chars)
     * - tipoProducto (4 chars)
     * - numeroProducto (12 chars)
     * - filler (24 chars) para completar 54 bytes
     */
    private ProgramParameter[] buildPer001Parameters(AS400 as400, TransferCommand command) {
        // Construir pinHeader (226 chars)
        String pinHeader = buildInHeader(
                nvl(command.getNombreOperacion(), "SERVICIO_TEST"),              // servicio (15)
                nvl(command.getIdTransaccion(), ""),                              // idTransaccion (50)
                nvl(command.getIdSesion(), ""),                                   // idSesion (50)
                OffsetDateTime.now().toString(),                                  // fechaHora (26)
                String.valueOf(command.getCanal() != null ? command.getCanal() : 0),  // canalAtencion (2)
                nvl(command.getCodPais(), "PA"),                                  // pais (2)
                nvl(command.getUsuario(), "SYSTEM"),                              // usuario (10)
                nvl(command.getCodIdioma(), "ES"),                                // idioma (2)
                nvl(command.getValOrigen(), "0.0.0.0")                            // ip (15)
        );
        
        // Construir pinBody (50 chars)
        String pinBody = buildInBody(
                nvl(command.getCodTipoIdentificacion(), ""),    // tipoIdentificacion (2)
                nvl(command.getValNumeroIdentificacion(), ""),  // numeroIdentificacion (12)
                nvl(command.getCodTipoProducto(), ""),          // tipoProducto (4)
                nvl(command.getValNumeroProducto(), "")         // numeroProducto (12)
        );

        LOGGER.debug("pinHeader construido (264 bytes): '{}'", pinHeader);
        LOGGER.debug("pinBody construido (54 bytes): '{}'", pinBody);

        // Convertidores de texto AS/400 (EBCDIC)
        AS400Text text264 = new AS400Text(264, as400.getCcsid());
        AS400Text text54 = new AS400Text(54, as400.getCcsid());

        // Parámetros IN/OUT (ajustar a tamaños de OUTHEADER y OUTBODY)
        byte[] p1Header = text264.toBytes(padRight(pinHeader, 264));
        byte[] p2Body = text54.toBytes(padRight(pinBody, 54));

        return new ProgramParameter[]{
                new ProgramParameter(p1Header, 264),    // IN/OUT: OUTHEADER (264 bytes)
                new ProgramParameter(p2Body, 54)        // IN/OUT: OUTBODY (54 bytes)
        };
    }
    
    /**
     * Construye el header de entrada para PER001P (264 bytes).
     */
    private String buildInHeader(String servicio, String idTransaccion, String idSesion,
                                  String fechaHora, String canalAtencion, String pais,
                                  String usuario, String idioma, String ip) {
        StringBuilder header = new StringBuilder();
        header.append(padRight(servicio, 15));          // servicio (15)
        header.append(padRight(idTransaccion, 50));     // idTransaccion (50)
        header.append(padRight(idSesion, 50));          // idSesion (50)
        header.append(padRight(fechaHora, 26));         // fechaHora (26)
        header.append(padRight(canalAtencion, 2));      // canalAtencion (2)
        header.append(padRight(pais, 2));               // pais (2)
        header.append(padRight(usuario, 10));           // usuario (10)
        header.append(padRight(idioma, 2));             // idioma (2)
        header.append(padRight(ip, 15));                // ip (15)
        header.append(padRight("", 92));                // filler (92) para 264 total
        return header.toString();
    }
    
    /**
     * Construye el body de entrada para PER001P (54 bytes).
     */
    private String buildInBody(String tipoIdentificacion, String numeroIdentificacion,
                                String tipoProducto, String numeroProducto) {
        StringBuilder body = new StringBuilder();
        body.append(padRight(tipoIdentificacion, 2));   // tipoIdentificacion (2)
        body.append(padRight(numeroIdentificacion, 12)); // numeroIdentificacion (12)
        body.append(padRight(tipoProducto, 4));         // tipoProducto (4)
        body.append(padRight(numeroProducto, 12));      // numeroProducto (12)
        body.append(padRight("", 24));                  // filler (24) para 54 total
        return body.toString();
    }
    
    /**
     * Rellena string con espacios a la derecha hasta alcanzar la longitud especificada.
     */
    private String padRight(String str, int length) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str.substring(0, length);
        }
        return String.format("%-" + length + "s", str);
    }

    /**
     * Parsea la respuesta exitosa del CL PER001P.
     * 
     * El CL retorna los parámetros actualizados:
     * - OUTHEADER: ACEPTABM(1) + ERROR(8) + MENSAJER(255) = 264 bytes
     * - OUTBODY: ONUMCOM(10) + OMONDEB(15) + OMONEDA(4) + OFECHOR(25) = 54 bytes
     */
    private TransferResult parseSuccessResponse(ProgramParameter[] parameters, TransferCommand command) {
        byte[] headerData = parameters[0].getOutputData();
        byte[] bodyData = parameters[1].getOutputData();

        // Parsear OUTHEADER (posiciones 1-based en RPG, 0-based en Java)
        AS400Text textAceptabm = new AS400Text(1, 37);
        AS400Text textError = new AS400Text(8, 37);
        AS400Text textMensajer = new AS400Text(255, 37);

        String aceptabm = ((String) textAceptabm.toObject(headerData, 0)).trim();
        String errorCode = ((String) textError.toObject(headerData, 1)).trim();
        String mensajer = ((String) textMensajer.toObject(headerData, 9)).trim();

        LOGGER.debug("OUTHEADER - ACEPTABM='{}', ERROR='{}', MENSAJER='{}'", 
                     aceptabm, errorCode, mensajer);

        // Parsear OUTBODY (posiciones 1-based en RPG, 0-based en Java)
        AS400Text textOnumcom = new AS400Text(10, 37);
        AS400Text textOmondeb = new AS400Text(15, 37);
        AS400Text textOmoneda = new AS400Text(4, 37);
        AS400Text textOfechor = new AS400Text(25, 37);

        String onumcom = ((String) textOnumcom.toObject(bodyData, 0)).trim();
        String omondeb = ((String) textOmondeb.toObject(bodyData, 10)).trim();
        String omoneda = ((String) textOmoneda.toObject(bodyData, 25)).trim();
        String ofechor = ((String) textOfechor.toObject(bodyData, 29)).trim();

        LOGGER.debug("OUTBODY - ONUMCOM='{}', OMONDEB='{}', OMONEDA='{}', OFECHOR='{}'",
                     onumcom, omondeb, omoneda, ofechor);

        // Construir resultado
        TransferResult result = new TransferResult();

        // DataHeader
        result.setNombreOperacion(command.getNombreOperacion());
        result.setTotal(command.getTotal());
        result.setCaracterAceptacion(aceptabm);
        result.setUltimoMensaje((short) 1);
        result.setIdTransaccion(command.getIdTransaccion());
        result.setCodMsgRespuesta(parseInteger(errorCode, 0));
        result.setMsgRespuesta(mensajer);

        // Data
        result.setValNumeroComprobante(onumcom);
        result.setValSecuencial(0L); // No hay secuencial en OUTBODY
        result.setFecHoraMovimiento(parseDateTime(ofechor));
        result.setValMonto(command.getValMonto());
        result.setCostoDeLaTransaccion(BigDecimal.ZERO);
        result.setValTasaCambio(command.getValTasaCambio());
        result.setValMontoDestino(command.getValMontoDestino());
        result.setCodMonedaTransaccion(omoneda.isEmpty() ? command.getCodMonedaProducto() : omoneda);

        return result;
    }

    /**
     * Parsea fecha/hora en formato AS/400 a OffsetDateTime.
     */
    private OffsetDateTime parseDateTime(String fechaHora) {
        if (fechaHora == null || fechaHora.isEmpty()) {
            return OffsetDateTime.now();
        }
        try {
            // Intentar parsear formato ISO-8601 u otro formato común
            return OffsetDateTime.parse(fechaHora);
        } catch (Exception e) {
            LOGGER.debug("No se pudo parsear fechaHora '{}', usando timestamp actual", fechaHora);
            return OffsetDateTime.now();
        }
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
