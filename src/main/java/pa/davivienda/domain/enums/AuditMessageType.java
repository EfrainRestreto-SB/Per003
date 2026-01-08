package pa.davivienda.domain.enums;

/**
 * Tipos de mensajes de auditoría para el sistema de logging funcional.
 * 
 * <p>
 * Cada transacción debe generar exactamente 4 registros en flujo normal:
 * <ul>
 *   <li>{@code ENTRADA}: Request recibido desde el BUS</li>
 *   <li>{@code TRAMA_OUT}: Mensaje transformado hacia AS/400 u otro servicio</li>
 *   <li>{@code TRAMA_IN}: Respuesta recibida desde AS/400 u otro servicio</li>
 *   <li>{@code SALIDA}: Response final enviado al BUS</li>
 *   <li>{@code ERROR}: Registro adicional en caso de excepción</li>
 * </ul>
 * </p>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 */
public enum AuditMessageType {
    /**
     * Request recibido desde el BUS (headers + body).
     * Primer punto de entrada a la transacción.
     */
    ENTRADA,
    
    /**
     * Mensaje transformado hacia AS/400 o servicio externo.
     * Representa la trama enviada después de la transformación.
     */
    TRAMA_OUT,
    
    /**
     * Respuesta recibida desde AS/400 o servicio externo.
     * Representa la trama recibida antes de transformarla al formato de salida.
     */
    TRAMA_IN,
    
    /**
     * Response final enviado al BUS (headers + body).
     * Último punto de salida de la transacción exitosa.
     */
    SALIDA,
    
    /**
     * Registro de error (excepción + stack trace).
     * Se genera cuando ocurre una excepción en cualquier punto del flujo.
     */
    ERROR
}
