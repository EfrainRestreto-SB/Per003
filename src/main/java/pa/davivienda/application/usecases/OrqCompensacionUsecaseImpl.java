package pa.davivienda.application.usecases;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;
import pa.davivienda.domain.entities.AuditLog;
import pa.davivienda.domain.enums.AuditMessageType;
import pa.davivienda.domain.interfaces.usecases.OrqCompensacionService;
import pa.davivienda.domain.ports.output.AuditPort;
import pa.davivienda.domain.ports.output.Per001ServicePort;
import pa.davivienda.transversal.utils.AuditUtils;

/**
 * Implementación del caso de uso de orquestación de compensación.
 * 
 * <p>
 * Esta clase contiene la lógica de negocio pura para la orquestación de transferencias
 * y compensaciones, independiente del protocolo REST y la capa de presentación.
 * </p>
 * 
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Procesar comandos de transferencia ({@link TransferCommand})</li>
 *   <li>Ejecutar la lógica de negocio de compensación</li>
 *   <li>Retornar resultados estructurados ({@link TransferResult})</li>
 * </ul>
 * </p>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 * @see OrqCompensacionService
 */
@ApplicationScoped
public class OrqCompensacionUsecaseImpl implements OrqCompensacionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(OrqCompensacionUsecaseImpl.class);
    
    @Inject
    AuditPort auditPort;
    
    @Inject
    Per001ServicePort per001Service;
    
    /**
     * Ejecuta una operación de transferencia/compensación.
     * 
     * <p>
     * Este método procesa un comando de transferencia, ejecutando la lógica de negocio
     * necesaria para realizar la compensación entre cuentas. El método registra logs
     * de auditoría al inicio y fin de la operación.
     * </p>
     * 
     * <p><b>Nota:</b> Actualmente, este método devuelve un resultado simulado.
     * La implementación real de la lógica de negocio está pendiente.</p>
     * 
     * @param command el comando que contiene los datos necesarios para ejecutar la transferencia,
     *                incluyendo identificador de transacción, tipo de concepto, monto,
     *                tasas de cambio y datos de origen/destino. No debe ser {@code null}.
     * @return un {@link TransferResult} con el resultado de la operación, incluyendo
     *         el comprobante generado, fecha/hora del movimiento, monto procesado
     *         y estado de la transacción.
     * @throws IllegalArgumentException si el comando es {@code null} o contiene datos inválidos
     * @see TransferCommand
     * @see TransferResult
     */
    @Override
    public TransferResult transfer(TransferCommand command) {
        LOG.info("Ejecutando transferencia - idTransaccion={}, concepto={}, monto={}", 
                 command.getIdTransaccion(), 
                 command.getCodTipoConcepto(), 
                 command.getValMonto());
        
        // 1. AUDITORÍA ENTRADA - Registrar request recibido
        auditPort.logAsync(AuditLog.builder()
                .idTransaccion(command.getIdTransaccion())
                .tipoMensaje(AuditMessageType.ENTRADA)
                .logCun(command.getValNumeroIdentificacion())  // CUN del cliente
                .logCanal(command.getCanal() != null ? String.valueOf(command.getCanal()) : null)
                .loginUser(command.getUsuario())
                .payload(AuditUtils.toJson(command))
                .estado("OK")
                .build());
        
        TransferResult result = null;
        
        try {
            // Determinar servicio destino según el concepto
            String concepto = command.getCodTipoConcepto();
            
            if ("COBPER".equals(concepto)) {
                // Cobro de membresía → PER001 (AS/400)
                LOG.info("Routing a PER001 (AS/400) para concepto COBPER");
                
                // AUDITORÍA TRAMA_OUT - Registrar invocación a PER001
                auditPort.logAsync(AuditLog.builder()
                        .idTransaccion(command.getIdTransaccion())
                        .tipoMensaje(AuditMessageType.TRAMA_OUT)
                        .logCun(command.getValNumeroIdentificacion())
                        .logCanal(command.getCanal() != null ? String.valueOf(command.getCanal()) : null)
                        .loginUser(command.getUsuario())
                        .payload(AuditUtils.toJson(command))
                        .estado("OK")
                        .build());
                
                // Invocar programa RPG PER001
                result = per001Service.processMembershipPayment(command);
                
                // AUDITORÍA TRAMA_IN - Registrar respuesta de PER001
                auditPort.logAsync(AuditLog.builder()
                        .idTransaccion(command.getIdTransaccion())
                        .tipoMensaje(AuditMessageType.TRAMA_IN)
                        .logCun(command.getValNumeroIdentificacion())
                        .logCanal(command.getCanal() != null ? String.valueOf(command.getCanal()) : null)
                        .loginUser(command.getUsuario())
                        .payload(AuditUtils.toJson(result))
                        .estado("OK")
                        .build());
                
            } else {
                // Otros conceptos (TRCPRO, TRCTER, etc.) - usar lógica simulada por ahora
                LOG.warn("Concepto {} no implementado, usando respuesta simulada", concepto);
                result = buildSimulatedResult(command);
            }
            
            LOG.info("Transferencia completada - comprobante={}", result.getValNumeroComprobante());
            
            // 2. AUDITORÍA SALIDA - Registrar response exitoso
            auditPort.logAsync(AuditLog.builder()
                    .idTransaccion(command.getIdTransaccion())
                    .tipoMensaje(AuditMessageType.SALIDA)
                    .logCun(command.getValNumeroIdentificacion())
                    .logCanal(command.getCanal() != null ? String.valueOf(command.getCanal()) : null)
                    .loginUser(command.getUsuario())
                    .payload(AuditUtils.toJson(result))
                    .estado("OK")
                    .build());
            
            return result;
            
        } catch (Exception e) {
            LOG.error("Error en transferencia - idTransaccion={}", command.getIdTransaccion(), e);
            
            // 3. AUDITORÍA ERROR - Registrar excepción
            auditPort.logAsync(AuditLog.builder()
                    .idTransaccion(command.getIdTransaccion())
                    .tipoMensaje(AuditMessageType.ERROR)
                    .logCun(command.getValNumeroIdentificacion())
                    .logCanal(command.getCanal() != null ? String.valueOf(command.getCanal()) : null)
                    .loginUser(command.getUsuario())
                    .payload(AuditUtils.exceptionToJson(e, "OrqCompensacionUsecaseImpl.transfer"))
                    .estado("ERROR")
                    .detalleError(e.getMessage())
                    .build());
            
            throw e;
        }
    }
    
    private TransferResult buildSimulatedResult(TransferCommand command) {
        TransferResult result = new TransferResult();
        
        // Header
        result.setNombreOperacion(command.getNombreOperacion());
        result.setTotal(command.getTotal());
        result.setCaracterAceptacion("B"); // B = OK
        result.setUltimoMensaje((short) 0);
        result.setIdTransaccion(command.getIdTransaccion());
        result.setCodMsgRespuesta(0);
        result.setMsgRespuesta("Transacción exitosa (simulada)");
        
        // Data
        result.setValNumeroComprobante("COMP-" + System.currentTimeMillis());
        result.setValSecuencial(System.currentTimeMillis());
        result.setFecHoraMovimiento(OffsetDateTime.now());
        result.setValMonto(command.getValMonto());
        result.setCostoDeLaTransaccion(BigDecimal.valueOf(2.50));
        result.setValTasaCambio(command.getValTasaCambio());
        result.setValMontoDestino(command.getValMontoDestino());
        result.setCodMonedaTransaccion(command.getCodMonedaDestino());
        
        return result;
    }
}

