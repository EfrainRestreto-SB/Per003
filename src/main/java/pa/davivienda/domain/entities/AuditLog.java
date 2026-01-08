package pa.davivienda.domain.entities;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pa.davivienda.domain.enums.AuditMessageType;

/**
 * Entidad de auditoría para logging funcional.
 * 
 * <p>
 * Representa un registro en la tabla PERUSRLIB.AUDIT_LOGS de DB2.
 * Inmutable usando Lombok {@code @Builder} para construcción fluida.
 * </p>
 * 
 * <p>
 * Cada transacción genera múltiples registros de auditoría:
 * <ul>
 *   <li>ENTRADA: Request original recibido</li>
 *   <li>TRAMA_OUT: Mensaje enviado a servicio externo</li>
 *   <li>TRAMA_IN: Respuesta recibida de servicio externo</li>
 *   <li>SALIDA: Response final al cliente</li>
 *   <li>ERROR: Información de excepciones</li>
 * </ul>
 * </p>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {
    
    /**
     * ID de transacción del BUS (viene en headers).
     * Permite correlacionar todos los registros de auditoría de una misma transacción.
     */
    private String idTransaccion;
    
    /**
     * Tipo de mensaje de auditoría.
     * 
     * @see AuditMessageType
     */
    private AuditMessageType tipoMensaje;
    
    /**
     * CUN del cliente (CUSCUN de tabla CUMST).
     * Identificador único del cliente en el sistema.
     */
    private String logCun;
    
    /**
     * Canal de origen de la transacción.
     * Ejemplos: BM (Banca Móvil), BI (Banca Internet), ATM, etc.
     */
    private String logCanal;
    
    /**
     * Usuario que ejecuta la operación.
     * Puede ser el usuario final o el usuario técnico del sistema.
     */
    private String loginUser;
    
    /**
     * Timestamp del evento de auditoría.
     * Por defecto se establece al momento de creación del objeto.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Payload del mensaje en formato JSON.
     * Contiene el cuerpo completo del mensaje (request, response, trama, etc.).
     */
    private String payload;
    
    /**
     * Hash SHA-256 del payload.
     * Permite verificar la integridad del mensaje auditado.
     * Se calcula automáticamente si no se proporciona.
     */
    private String payloadHash;
    
    /**
     * Estado de la operación.
     * Valores típicos: "OK", "ERROR".
     */
    @Builder.Default
    private String estado = "OK";
    
    /**
     * Detalle del error (si aplica).
     * Contiene el mensaje de error y opcionalmente el stack trace.
     */
    private String detalleError;
    
    /**
     * Origen del microservicio que genera el registro.
     * Identifica el sistema fuente de la auditoría.
     */
    @Builder.Default
    private String origen = "PER003";
    
    /**
     * Servicio que genera el registro de auditoría.
     * Nombre del microservicio o componente específico.
     */
    @Builder.Default
    private String servicio = "PER003";
    
    /**
     * Sistema/Usuario creador del registro.
     * Identifica el proceso o servicio que creó el registro de auditoría.
     */
    @Builder.Default
    private String createdBy = "PER003-SERVICE";
}
