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

            // Log de parámetros antes de ejecutar el CL
            logParametersBeforeCall(parameters);

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
     * - P1 (INPUT): INHEADER (215 bytes) - Cabecera de entrada
     * - P2 (INPUT): INBODY (50 bytes) - Cuerpo de entrada
     * - P3 (OUTPUT): OUTHEADER (264 bytes) - ACEPTABM(1) + ERROR(8) + MENSAJER(255)
     * - P4 (OUTPUT): OUTBODY (54 bytes) - ONUMCOM(10) + OMONDEB(15) + OMONEDA(4) + OFECHOR(25)
     * 
     * INHEADER (215 bytes):
     * - SERVICIO (20 chars)
     * - IDTRX (36 chars)
     * - IDSESION (36 chars)
     * - FECASIS (30 chars)
     * - CANAL (10 chars)
     * - PAIS (3 chars)
     * - USUARIO1 (20 chars)
     * - IDIOMA (10 chars)
     * - IPCLIENTE (50 chars)
     * 
     * INBODY (50 bytes):
     * - ITipId (4 chars)
     * - INumId (30 chars)
     * - ITippRo (4 chars)
     * - ICuenTa (12 chars)
     */
    private ProgramParameter[] buildPer001Parameters(AS400 as400, TransferCommand command) {
        // Construir INHEADER (215 bytes)
        String inHeader = buildInHeader(
                nvl(command.getNombreOperacion(), "SERVICIO_TEST"),              // SERVICIO (20)
                nvl(command.getIdTransaccion(), ""),                              // IDTRX (36)
                nvl(command.getIdSesion(), ""),                                   // IDSESION (36)
                OffsetDateTime.now().toString(),                                  // FECASIS (30)
                String.valueOf(command.getCanal() != null ? command.getCanal() : 0),  // CANAL (10)
                nvl(command.getCodPais(), "PA"),                                  // PAIS (3)
                nvl(command.getUsuario(), "SYSTEM"),                              // USUARIO1 (20)
                nvl(command.getCodIdioma(), "ES"),                                // IDIOMA (10)
                nvl(command.getValOrigen(), "0.0.0.0")                            // IPCLIENTE (50)
        );
        
        // Construir INBODY (50 bytes)
        String inBody = buildInBody(
                nvl(command.getCodTipoIdentificacion(), ""),    // ITipId (4)
                nvl(command.getValNumeroIdentificacion(), ""),  // INumId (30)
                nvl(command.getCodTipoProducto(), ""),          // ITippRo (4)
                nvl(command.getValNumeroProducto(), "")         // ICuenTa (12)
        );

        LOGGER.debug("INHEADER construido (215 bytes): '{}'", inHeader);
        LOGGER.debug("INBODY construido (50 bytes): '{}'", inBody);
        
        // Log detallado de cada campo INHEADER
        logInHeaderDetails(command);
        
        // *** NUEVO: Capturar valores String ANTES de conversión a EBCDIC ***
        logValoresAntesDeConversion(command, inHeader, inBody);

        // Convertidores de texto AS/400 (EBCDIC)
        AS400Text text215 = new AS400Text(215, as400.getCcsid());
        AS400Text text50 = new AS400Text(50, as400.getCcsid());
        AS400Text text264 = new AS400Text(264, as400.getCcsid());
        AS400Text text54 = new AS400Text(54, as400.getCcsid());

        // Parámetros: INPUT (INHEADER, INBODY) y OUTPUT (OUTHEADER, OUTBODY)
        byte[] p1InHeader = text215.toBytes(inHeader);
        byte[] p2InBody = text50.toBytes(inBody);

        return new ProgramParameter[]{
                new ProgramParameter(p1InHeader),           // P1: INHEADER (215 bytes) INPUT
                new ProgramParameter(p2InBody),             // P2: INBODY (50 bytes) INPUT
                new ProgramParameter(264),                  // P3: OUTHEADER (264 bytes) OUTPUT
                new ProgramParameter(54)                    // P4: OUTBODY (54 bytes) OUTPUT
        };
    }
    
    /**
     * Loguea los 4 parámetros justo antes de ejecutar el CL PER001P.
     */
    private void logParametersBeforeCall(ProgramParameter[] parameters) {
        LOGGER.info("╔════════════════════════════════════════════════════════════════");
        LOGGER.info("║ PARÁMETROS ENVIADOS AL CL PER001P (Justo antes de program.run)");
        LOGGER.info("╠════════════════════════════════════════════════════════════════");
        
        for (int i = 0; i < parameters.length; i++) {
            ProgramParameter param = parameters[i];
            byte[] inputData = param.getInputData();
            
            if (inputData != null) {
                // INPUT parameter
                String content = new String(inputData);
                LOGGER.info("║ P{} (INPUT): {} bytes", (i + 1), inputData.length);
                LOGGER.info("║   Contenido: '{}'", content);
                LOGGER.info("║   Hex: {}", bytesToHex(inputData));
            } else {
                // OUTPUT parameter
                LOGGER.info("║ P{} (OUTPUT): {} bytes (buffer vacío esperando respuesta)", (i + 1), param.getOutputDataLength());
            }
            LOGGER.info("╠════════════════════════════════════════════════════════════════");
        }
        
        LOGGER.info("║ TOTAL: {} parámetros preparados para enviar", parameters.length);
        LOGGER.info("╚════════════════════════════════════════════════════════════════");
    }
    
    /**
     * Convierte bytes a representación hexadecimal.
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int maxBytes = Math.min(bytes.length, 50); // Limitar a primeros 50 bytes para no saturar el log
        for (int i = 0; i < maxBytes; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        if (bytes.length > maxBytes) {
            sb.append("... (").append(bytes.length - maxBytes).append(" bytes más)");
        }
        return sb.toString();
    }

    /**
     * Loguea los valores String ANTES de la conversión a bytes EBCDIC.
     */
    private void logValoresAntesDeConversion(TransferCommand command, String inHeaderStr, String inBodyStr) {
        LOGGER.info("╔═══════════════════════════════════════════════════════════════════════════════");
        LOGGER.info("║ VALORES STRING ANTES DE CONVERSIÓN A EBCDIC");
        LOGGER.info("╠═══════════════════════════════════════════════════════════════════════════════");
        LOGGER.info("║");
        LOGGER.info("║ ┌─ INHEADER (215 bytes) ────────────────────────────────────────────────────");
        LOGGER.info("║ │");
        LOGGER.info("║ │  SERVICIO (20):      '{}'", padRight(nvl(command.getNombreOperacion(), "SERVICIO_TEST"), 20));
        LOGGER.info("║ │  IDTRX (36):         '{}'", padRight(nvl(command.getIdTransaccion(), ""), 36));
        LOGGER.info("║ │  IDSESION (36):      '{}'", padRight(nvl(command.getIdSesion(), ""), 36));
        LOGGER.info("║ │  FECASIS (30):       '{}'", padRight(OffsetDateTime.now().toString(), 30));
        LOGGER.info("║ │  CANAL (10):         '{}'", padRight(String.valueOf(command.getCanal() != null ? command.getCanal() : 0), 10));
        LOGGER.info("║ │  PAIS (3):           '{}'", padRight(nvl(command.getCodPais(), "PA"), 3));
        LOGGER.info("║ │  USUARIO1 (20):      '{}'", padRight(nvl(command.getUsuario(), "SYSTEM"), 20));
        LOGGER.info("║ │  IDIOMA (10):        '{}'", padRight(nvl(command.getCodIdioma(), "ES"), 10));
        LOGGER.info("║ │  IPCLIENTE (50):     '{}'", padRight(nvl(command.getValOrigen(), "0.0.0.0"), 50));
        LOGGER.info("║ │");
        LOGGER.info("║ │  String completo (215 chars): '{}'", inHeaderStr);
        LOGGER.info("║ │  Longitud real: {} caracteres", inHeaderStr.length());
        LOGGER.info("║ └───────────────────────────────────────────────────────────────────────────");
        LOGGER.info("║");
        LOGGER.info("║ ┌─ INBODY (50 bytes) ──────────────────────────────────────────────────────");
        LOGGER.info("║ │");
        LOGGER.info("║ │  ITipId (4):         '{}'", padRight(nvl(command.getCodTipoIdentificacion(), ""), 4));
        LOGGER.info("║ │  INumId (30):        '{}'", padRight(nvl(command.getValNumeroIdentificacion(), ""), 30));
        LOGGER.info("║ │  ITippRo (4):        '{}'", padRight(nvl(command.getCodTipoProducto(), ""), 4));
        LOGGER.info("║ │  ICuenTa (12):       '{}'", padRight(nvl(command.getValNumeroProducto(), ""), 12));
        LOGGER.info("║ │");
        LOGGER.info("║ │  String completo (50 chars): '{}'", inBodyStr);
        LOGGER.info("║ │  Longitud real: {} caracteres", inBodyStr.length());
        LOGGER.info("║ └───────────────────────────────────────────────────────────────────────────");
        LOGGER.info("║");
        LOGGER.info("║ NOTA: Estos valores String serán convertidos a bytes EBCDIC (CCSID 37)");
        LOGGER.info("║       antes de ser enviados al programa CL PER001P en el AS/400");
        LOGGER.info("╚═══════════════════════════════════════════════════════════════════════════════");
    }
    
    /**
     * Loguea los detalles de cada campo del INHEADER para depuración.
     */
    private void logInHeaderDetails(TransferCommand command) {
        LOGGER.info("=== INHEADER - Detalle de campos enviados al CL ===");
        LOGGER.info("  SERVICIO (20):    '{}'", padRight(nvl(command.getNombreOperacion(), "SERVICIO_TEST"), 20));
        LOGGER.info("  IDTRX (36):       '{}'", padRight(nvl(command.getIdTransaccion(), ""), 36));
        LOGGER.info("  IDSESION (36):    '{}'", padRight(nvl(command.getIdSesion(), ""), 36));
        LOGGER.info("  FECASIS (30):     '{}'", padRight(OffsetDateTime.now().toString(), 30));
        LOGGER.info("  CANAL (10):       '{}'", padRight(String.valueOf(command.getCanal() != null ? command.getCanal() : 0), 10));
        LOGGER.info("  PAIS (3):         '{}'", padRight(nvl(command.getCodPais(), "PA"), 3));
        LOGGER.info("  USUARIO1 (20):    '{}'", padRight(nvl(command.getUsuario(), "SYSTEM"), 20));
        LOGGER.info("  IDIOMA (10):      '{}'", padRight(nvl(command.getCodIdioma(), "ES"), 10));
        LOGGER.info("  IPCLIENTE (50):   '{}'", padRight(nvl(command.getValOrigen(), "0.0.0.0"), 50));
        LOGGER.info("=== INBODY - Detalle de campos enviados al CL ===");
        LOGGER.info("  ITipId (4):       '{}'", padRight(nvl(command.getCodTipoIdentificacion(), ""), 4));
        LOGGER.info("  INumId (30):      '{}'", padRight(nvl(command.getValNumeroIdentificacion(), ""), 30));
        LOGGER.info("  ITippRo (4):      '{}'", padRight(nvl(command.getCodTipoProducto(), ""), 4));
        LOGGER.info("  ICuenTa (12):     '{}'", padRight(nvl(command.getValNumeroProducto(), ""), 12));
        LOGGER.info("===================================================");
    }
    
    /**
     * Construye el header de entrada para PER001P (215 bytes).
     * INHEADER: SERVICIO(20) + IDTRX(36) + IDSESION(36) + FECASIS(30) + CANAL(10) + PAIS(3) + USUARIO1(20) + IDIOMA(10) + IPCLIENTE(50)
     */
    private String buildInHeader(String servicio, String idTransaccion, String idSesion,
                                  String fechaHora, String canalAtencion, String pais,
                                  String usuario, String idioma, String ip) {
        StringBuilder header = new StringBuilder();
        header.append(padRight(servicio, 20));          // SERVICIO (20)
        header.append(padRight(idTransaccion, 36));     // IDTRX (36)
        header.append(padRight(idSesion, 36));          // IDSESION (36)
        header.append(padRight(fechaHora, 30));         // FECASIS (30)
        header.append(padRight(canalAtencion, 10));     // CANAL (10)
        header.append(padRight(pais, 3));               // PAIS (3)
        header.append(padRight(usuario, 20));           // USUARIO1 (20)
        header.append(padRight(idioma, 10));            // IDIOMA (10)
        header.append(padRight(ip, 50));                // IPCLIENTE (50)
        return header.toString();
    }
    
    /**
     * Construye el body de entrada para PER001P (50 bytes).
     * INBODY: ITipId(4) + INumId(30) + ITippRo(4) + ICuenTa(12)
     */
    private String buildInBody(String tipoIdentificacion, String numeroIdentificacion,
                                String tipoProducto, String numeroProducto) {
        StringBuilder body = new StringBuilder();
        body.append(padRight(tipoIdentificacion, 4));    // ITipId (4)
        body.append(padRight(numeroIdentificacion, 30)); // INumId (30)
        body.append(padRight(tipoProducto, 4));          // ITippRo (4)
        body.append(padRight(numeroProducto, 12));       // ICuenTa (12)
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
     * El CL retorna los parámetros OUTPUT:
     * - P3: OUTHEADER: ACEPTABM(1) + ERROR(8) + MENSAJER(255) = 264 bytes
     * - P4: OUTBODY: ONUMCOM(10) + OMONDEB(15) + OMONEDA(4) + OFECHOR(25) = 54 bytes
     */
    private TransferResult parseSuccessResponse(ProgramParameter[] parameters, TransferCommand command) {
        byte[] headerData = parameters[2].getOutputData();  // P3: OUTHEADER
        byte[] bodyData = parameters[3].getOutputData();    // P4: OUTBODY

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

        // Data - Mapeo desde AS400
        result.setValNumeroComprobante(onumcom);
        result.setValSecuencial(0L); // No hay secuencial en OUTBODY
        result.setFecHoraMovimiento(parseDateTime(ofechor)); // OFECHOR de AS400
        result.setValMonto(parseBigDecimal(omondeb, BigDecimal.ZERO)); // OMONDEB de AS400
        result.setCostoDeLaTransaccion(null); // Retorna null según especificación
        result.setValTasaCambio(null); // Retorna null según especificación
        result.setValMontoDestino(null); // Retorna null según especificación
        result.setCodMonedaTransaccion(omoneda.isEmpty() ? command.getCodMonedaProducto() : omoneda); // OMONEDA de AS400

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
     * Parsea un string a BigDecimal de forma segura.
     */
    private BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.debug("No se pudo parsear BigDecimal '{}', usando valor por defecto", value);
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
