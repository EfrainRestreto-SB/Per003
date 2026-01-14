package pa.davivienda.persistence.adapters;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import pa.davivienda.domain.entities.AuditLog;
import pa.davivienda.domain.ports.output.AuditPort;
import pa.davivienda.transversal.utils.AuditUtils;

/**
 * Adapter JDBC para auditoría usando StatelessSession de Hibernate.
 * 
 * <p>
 * Implementación del puerto {@link AuditPort} siguiendo arquitectura hexagonal.
 * Persiste registros de auditoría en la tabla PERUSRLIB.AUDIT_LOGS de DB2.
 * </p>
 * 
 * <p><b>Características:</b></p>
 * <ul>
 *   <li>Usa StatelessSession para operaciones de escritura sin caché</li>
 *   <li>Retry automático con backoff exponencial (3 intentos)</li>
 *   <li>Transaccionalmente independiente del flujo principal</li>
 *   <li>No propaga excepciones al flujo de negocio</li>
 *   <li>Soporte para operaciones síncronas y asíncronas</li>
 * </ul>
 * 
 * <p>
 * El uso de StatelessSession es intencional para:
 * <ul>
 *   <li>Evitar overhead de caché de primer nivel</li>
 *   <li>Garantizar independencia transaccional</li>
 *   <li>Optimizar rendimiento en escrituras de auditoría</li>
 * </ul>
 * </p>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 */
@ApplicationScoped
public class AuditAdapterJdbc implements AuditPort {
    
    /**
     * Número máximo de reintentos en caso de fallo.
     */
    private static final int MAX_RETRIES = 3;
    
    /**
     * Delay inicial entre reintentos en milisegundos.
     * Se aplica backoff exponencial: 100ms, 200ms, 300ms.
     */
    private static final long INITIAL_RETRY_DELAY_MS = 100;
    
    @Inject
    EntityManager entityManager;
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Implementación síncrona con retry logic.
     * Si falla después de todos los reintentos, solo loguea el error.
     * </p>
     */
    @Override
    public void log(AuditLog auditLog) {
        try {
            insertWithRetry(auditLog, MAX_RETRIES);
        } catch (Exception e) {
            // No propagar excepción - solo loguear
            Log.errorf("Failed to insert audit log after %d retries: %s", 
                      MAX_RETRIES, e.getMessage());
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Implementación asíncrona usando CompletableFuture.
     * Retorna inmediatamente sin bloquear el flujo principal.
     * </p>
     */
    @Override
    public void logAsync(AuditLog auditLog) {
        CompletableFuture.runAsync(() -> log(auditLog));
    }
    
    /**
     * Inserta el log con retry logic y backoff exponencial.
     * 
     * <p>
     * Implementa reintentos automáticos con delay creciente:
     * <ul>
     *   <li>Intento 1: inmediato</li>
     *   <li>Intento 2: espera 100ms</li>
     *   <li>Intento 3: espera 200ms</li>
     * </ul>
     * </p>
     * 
     * @param auditLog log a insertar
     * @param retriesLeft intentos restantes
     * @throws Exception si falla después de todos los reintentos
     */
    private void insertWithRetry(AuditLog auditLog, int retriesLeft) throws Exception {
        try {
            insertAuditLog(auditLog);
        } catch (Exception e) {
            if (retriesLeft > 1) {
                // Calcular delay exponencial
                long delay = INITIAL_RETRY_DELAY_MS * (MAX_RETRIES - retriesLeft + 1);
                
                Log.warnf("Audit insert failed, retrying in %dms. Retries left: %d. Error: %s",
                         delay, retriesLeft - 1, e.getMessage());
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                
                insertWithRetry(auditLog, retriesLeft - 1);
            } else {
                // Último intento falló
                throw e;
            }
        }
    }
    
    /**
     * Realiza el INSERT en PERUSRLIB.AUDIT_LOGS usando StatelessSession.
     * 
     * <p>
     * Ejecuta el INSERT en una transacción independiente que no afecta
     * ni es afectada por la transacción principal del caso de uso.
     * </p>
     * 
     * <p>
     * Si el payloadHash no está calculado, se calcula automáticamente
     * usando SHA-256.
     * </p>
     * 
     * @param auditLog log a insertar
     * @throws Exception si falla la inserción
     */
    private void insertAuditLog(AuditLog auditLog) {
        SessionFactory sf = entityManager
                .unwrap(Session.class)
                .getSessionFactory();
        
        try (StatelessSession ss = sf.openStatelessSession()) {
            
            // Iniciar transacción independiente
            ss.beginTransaction();
            
            try {
                // SQL INSERT - Tabla de auditoría en DB2
                String sql = """
                    INSERT INTO PERUSRLIB.AUDIT_LOGS (
                        ID_TRANSACCION,
                        TIPO_MENSAJE,
                        LOG_CUN,
                        LOG_CANAL,
                        LOGIN_USER,
                        TS,
                        PAYLOAD,
                        PAYLOAD_HASH,
                        ESTADO,
                        DETALLE_ERROR,
                        ORIGEN,
                        SERVICIO,
                        CREATED_BY
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                
                // Calcular hash si no está calculado
                String payloadHash = auditLog.getPayloadHash();
                if (payloadHash == null && auditLog.getPayload() != null) {
                    payloadHash = AuditUtils.calculateSHA256(auditLog.getPayload());
                }
                
                // Ejecutar INSERT con parámetros bind
                ss.createNativeMutationQuery(sql)
                    .setParameter(1, auditLog.getIdTransaccion())
                    .setParameter(2, auditLog.getTipoMensaje().name())
                    .setParameter(3, auditLog.getLogCun())
                    .setParameter(4, auditLog.getLogCanal())
                    .setParameter(5, auditLog.getLoginUser())
                    .setParameter(6, Timestamp.from(auditLog.getTimestamp()))
                    .setParameter(7, auditLog.getPayload())
                    .setParameter(8, payloadHash)
                    .setParameter(9, auditLog.getEstado())
                    .setParameter(10, auditLog.getDetalleError())
                    .setParameter(11, auditLog.getOrigen())
                    .setParameter(12, auditLog.getServicio())
                    .setParameter(13, auditLog.getCreatedBy())
                    .executeUpdate();
                
                // Commit transacción
                ss.getTransaction().commit();
                
                Log.debugf("Audit log inserted: type=%s, trx=%s", 
                          auditLog.getTipoMensaje(), auditLog.getIdTransaccion());
                
            } catch (Exception e) {
                // Rollback en caso de error
                if (ss.getTransaction().isActive()) {
                    ss.getTransaction().rollback();
                }
                throw e;
            }
        }
    }
}
