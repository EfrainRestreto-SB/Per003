package pa.davivienda.domain.ports.output;

import pa.davivienda.domain.entities.AuditLog;

/**
 * Puerto de salida para auditoría (Arquitectura Hexagonal).
 * 
 * <p>
 * Define el contrato para registrar logs de auditoría funcional en el sistema.
 * La implementación concreta estará en la capa de persistencia.
 * </p>
 * 
 * <p><b>Reglas de Implementación:</b></p>
 * <ul>
 *   <li>No debe romper el flujo principal si falla el registro</li>
 *   <li>Debe implementar retry logic (mínimo 3 intentos)</li>
 *   <li>Debe ser transaccionalmente independiente</li>
 *   <li>No debe propagar excepciones al flujo de negocio</li>
 * </ul>
 * 
 * <p>
 * Este puerto permite la inversión de dependencias: el dominio define el contrato
 * y la capa de persistencia lo implementa, permitiendo cambiar la implementación
 * (DB2, MongoDB, Kafka, etc.) sin afectar la lógica de negocio.
 * </p>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 */
public interface AuditPort {
    
    /**
     * Registra un log de auditoría de forma síncrona.
     * 
     * <p>
     * El método debe completarse antes de continuar con el flujo.
     * Si falla el registro después de los reintentos configurados,
     * NO debe propagar la excepción al flujo principal.
     * Solo debe loguear el error internamente.
     * </p>
     * 
     * <p><b>Uso recomendado:</b> Para auditoría crítica donde se requiere
     * confirmación del registro antes de continuar.</p>
     * 
     * @param auditLog datos del log a registrar. No debe ser {@code null}.
     */
    void log(AuditLog auditLog);
    
    /**
     * Registra un log de auditoría de forma asíncrona (fire-and-forget).
     * 
     * <p>
     * El método retorna inmediatamente sin esperar la confirmación del registro.
     * Útil para no bloquear el flujo principal en operaciones de auditoría
     * no críticas.
     * </p>
     * 
     * <p><b>Uso recomendado:</b> Para auditoría no crítica o logs de alto
     * volumen donde el rendimiento es prioritario.</p>
     * 
     * <p><b>Nota:</b> Los errores en el registro asíncrono solo se loguean,
     * no se notifican al llamador.</p>
     * 
     * @param auditLog datos del log a registrar. No debe ser {@code null}.
     */
    void logAsync(AuditLog auditLog);
}
