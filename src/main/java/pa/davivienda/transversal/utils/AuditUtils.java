package pa.davivienda.transversal.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.domain.entities.AuditLog;
import pa.davivienda.domain.enums.AuditMessageType;

/**
 * Utilidades para el sistema de auditoría.
 * 
 * <p>
 * Proporciona métodos helper para:
 * <ul>
 *   <li>Serialización de objetos a JSON</li>
 *   <li>Cálculo de hash SHA-256 para integridad</li>
 *   <li>Manejo de timestamps y formato de fechas</li>
 *   <li>Construcción de payloads estructurados</li>
 *   <li>Creación simplificada de logs de auditoría</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Todas las operaciones son thread-safe y sin efectos secundarios.
 * El ObjectMapper es configurado una sola vez y reutilizado.
 * </p>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 */
public class AuditUtils {
    
    /**
     * ObjectMapper configurado para serialización JSON.
     * Incluye soporte para tipos de fecha de Java 8+ (JSR-310).
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    /**
     * Constructor privado para prevenir instanciación.
     * Esta clase solo contiene métodos estáticos utilitarios.
     */
    private AuditUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Convierte un objeto a JSON string.
     * 
     * <p>
     * Si la serialización falla, retorna un JSON con el mensaje de error
     * en lugar de lanzar una excepción, para evitar romper el flujo de auditoría.
     * </p>
     * 
     * @param obj objeto a serializar. Puede ser {@code null}.
     * @return JSON string del objeto, o "{}" si obj es null, o un JSON de error si falla
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize: " + escapeJson(e.getMessage()) + "\"}";
        }
    }
    
    /**
     * Calcula el hash SHA-256 de un string.
     * 
     * <p>
     * Útil para verificar la integridad de payloads sin almacenar
     * el contenido completo o para detectar modificaciones.
     * </p>
     * 
     * @param input string a hashear. Si es {@code null} o vacío, retorna "".
     * @return hash SHA-256 en formato hexadecimal (64 caracteres), o "" si input es null/vacío
     */
    public static String calculateSHA256(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en JDK 11+
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Convierte array de bytes a string hexadecimal.
     * 
     * @param bytes array de bytes a convertir. No debe ser {@code null}.
     * @return string hexadecimal en minúsculas
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Crea un JSON con información de una query SQL para auditoría.
     * 
     * <p>
     * Útil para auditar operaciones de base de datos sin exponer
     * datos sensibles directamente en los logs.
     * </p>
     * 
     * @param queryName nombre identificador de la query
     * @param query SQL query ejecutada
     * @param params parámetros de la query (valores bind)
     * @return JSON string con la información de la query
     */
    public static String queryToJson(String queryName, String query, Object... params) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"queryName\":\"").append(escapeJson(queryName)).append("\",");
        json.append("\"query\":\"").append(escapeJson(query)).append("\",");
        json.append("\"params\":[");
        
        for (int i = 0; i < params.length; i++) {
            if (i > 0) json.append(",");
            if (params[i] == null) {
                json.append("null");
            } else {
                json.append("\"").append(escapeJson(String.valueOf(params[i]))).append("\"");
            }
        }
        
        json.append("]}");
        return json.toString();
    }
    
    /**
     * Crea un JSON con información de una excepción para auditoría.
     * 
     * <p>
     * Incluye el tipo de excepción, mensaje y stack trace limitado
     * para evitar payloads excesivamente grandes en la base de datos.
     * </p>
     * 
     * @param ex excepción a serializar. No debe ser {@code null}.
     * @param context contexto donde ocurrió la excepción
     * @return JSON string con la información de la excepción
     */
    public static String exceptionToJson(Exception ex, String context) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"context\":\"").append(escapeJson(context)).append("\",");
        json.append("\"exception\":\"").append(ex.getClass().getSimpleName()).append("\",");
        json.append("\"message\":\"").append(escapeJson(ex.getMessage())).append("\",");
        json.append("\"stackTrace\":\"").append(escapeJson(getStackTrace(ex))).append("\"");
        json.append("}");
        return json.toString();
    }
    
    /**
     * Obtiene el stack trace completo de una excepción, limitado a 20 líneas.
     * 
     * <p>
     * La limitación evita que el stack trace ocupe demasiado espacio
     * en la base de datos, manteniendo suficiente información para debugging.
     * </p>
     * 
     * @param ex excepción de la cual extraer el stack trace
     * @return stack trace como string, limitado a 20 líneas
     */
    private static String getStackTrace(Exception ex) {
        if (ex == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(ex.toString()).append("\n");
        
        StackTraceElement[] elements = ex.getStackTrace();
        int limit = Math.min(elements.length, 20);
        
        for (int i = 0; i < limit; i++) {
            sb.append("  at ").append(elements[i].toString()).append("\n");
        }
        
        if (elements.length > 20) {
            sb.append("  ... (").append(elements.length - 20).append(" more lines truncated)");
        }
        
        return sb.toString();
    }
    
    /**
     * Escapa caracteres especiales para inclusión segura en JSON.
     * 
     * <p>
     * Maneja: backslash, comillas, newline, carriage return, tab.
     * Previene inyección de JSON malformado.
     * </p>
     * 
     * @param str string a escapar. Si es {@code null}, retorna "".
     * @return string escapado, seguro para incluir en JSON
     */
    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Crea un log de auditoría base desde un comando de transferencia.
     * 
     * <p>
     * Este método extrae los campos comunes del comando y crea un builder
     * de AuditLog pre-configurado. Esto elimina duplicación de código
     * al crear múltiples logs de auditoría para la misma transacción.
     * </p>
     * 
     * @param command comando de transferencia del cual extraer los datos comunes
     * @param messageType tipo de mensaje de auditoría (ENTRADA, SALIDA, TRAMA_IN, TRAMA_OUT, ERROR)
     * @return builder de AuditLog con los campos comunes configurados
     */
    public static AuditLog.AuditLogBuilder createAuditLog(TransferCommand command, AuditMessageType messageType) {
        return AuditLog.builder()
                .idTransaccion(command.getIdTransaccion())
                .tipoMensaje(messageType)
                .logCun(command.getValNumeroIdentificacion())
                .logCanal(command.getCanal() != null ? String.valueOf(command.getCanal()) : null)
                .loginUser(command.getUsuario());
    }
    
    /**
     * Crea un log de auditoría de entrada con payload y estado OK.
     * 
     * @param command comando de transferencia
     * @param payload objeto a serializar como payload JSON
     * @return AuditLog completo listo para registrar
     */
    public static AuditLog createEntradaLog(TransferCommand command, Object payload) {
        return createAuditLog(command, AuditMessageType.ENTRADA)
                .payload(toJson(payload))
                .estado("OK")
                .build();
    }
    
    /**
     * Crea un log de auditoría de salida con payload y estado OK.
     * 
     * @param command comando de transferencia
     * @param payload objeto a serializar como payload JSON
     * @return AuditLog completo listo para registrar
     */
    public static AuditLog createSalidaLog(TransferCommand command, Object payload) {
        return createAuditLog(command, AuditMessageType.SALIDA)
                .payload(toJson(payload))
                .estado("OK")
                .build();
    }
    
    /**
     * Crea un log de auditoría de trama saliente (TRAMA_OUT) con payload y estado OK.
     * 
     * @param command comando de transferencia
     * @param payload objeto a serializar como payload JSON
     * @return AuditLog completo listo para registrar
     */
    public static AuditLog createTramaOutLog(TransferCommand command, Object payload) {
        return createAuditLog(command, AuditMessageType.TRAMA_OUT)
                .payload(toJson(payload))
                .estado("OK")
                .build();
    }
    
    /**
     * Crea un log de auditoría de trama entrante (TRAMA_IN) con payload y estado OK.
     * 
     * @param command comando de transferencia
     * @param payload objeto a serializar como payload JSON
     * @return AuditLog completo listo para registrar
     */
    public static AuditLog createTramaInLog(TransferCommand command, Object payload) {
        return createAuditLog(command, AuditMessageType.TRAMA_IN)
                .payload(toJson(payload))
                .estado("OK")
                .build();
    }
    
    /**
     * Crea un log de auditoría de error con información de la excepción.
     * 
     * @param command comando de transferencia
     * @param ex excepción ocurrida
     * @param context contexto donde ocurrió el error
     * @return AuditLog completo listo para registrar
     */
    public static AuditLog createErrorLog(TransferCommand command, Exception ex, String context) {
        return createAuditLog(command, AuditMessageType.ERROR)
                .payload(exceptionToJson(ex, context))
                .estado("ERROR")
                .detalleError(ex.getMessage())
                .build();
    }
}
