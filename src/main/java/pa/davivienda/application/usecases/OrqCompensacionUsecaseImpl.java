package pa.davivienda.application.usecases;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;
import pa.davivienda.application.validators.ChannelConceptValidator;
import pa.davivienda.domain.interfaces.usecases.OrqCompensacionService;
import pa.davivienda.domain.ports.output.AuditPort;
import pa.davivienda.domain.ports.output.Per001ServicePort;
import pa.davivienda.transversal.constants.TransactionConstants;
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
    
    @Inject
    ChannelConceptValidator channelConceptValidator;
    
    @Inject
    MeterRegistry meterRegistry;
    
    /**
     * Ejecuta una operación de transferencia/compensación.
     * 
     * <p>
     * Este método orquesta el flujo completo de una transferencia: validación,
     * auditoría de entrada, enrutamiento al servicio correspondiente según el concepto,
     * recolección de métricas, auditoría de salida y manejo de errores.
     * </p>
     * 
     * @param command el comando que contiene los datos necesarios para ejecutar la transferencia,
     *                incluyendo identificador de transacción, tipo de concepto, monto,
     *                tasas de cambio y datos de origen/destino. No debe ser {@code null}.
     * @return un {@link TransferResult} con el resultado de la operación, incluyendo
     *         el comprobante generado, fecha/hora del movimiento, monto procesado
     *         y estado de la transacción.
     * @throws IllegalArgumentException si el comando es {@code null} o contiene datos inválidos
     * @throws RuntimeException si ocurre un error durante el procesamiento de la transferencia
     * @see TransferCommand
     * @see TransferResult
     */
    @Override
    @Timed(value = "transfer.time", description = "Tiempo de ejecución de transferencias", percentiles = {0.5, 0.95, 0.99})
    @Counted(value = "transfer.total", description = "Contador total de transferencias")
    public TransferResult transfer(TransferCommand command) {
        logTransferStart(command);
        
        validateRequest(command);
        auditEntryRequest(command);
        
        try {
            TransferResult result = routeAndProcessTransfer(command);
            
            recordSuccessMetrics(command.getCodTipoConcepto(), command.getValMonto());
            logTransferCompletion(result);
            auditSuccessResponse(command, result);
            
            return result;
            
        } catch (Exception e) {
            handleTransferError(command, e);
            throw e;
        }
    }
    
    /**
     * Registra el inicio de la transferencia en los logs.
     */
    private void logTransferStart(TransferCommand command) {
        LOG.info("Ejecutando transferencia - idTransaccion={}, canal={}, concepto={}, monto={}", 
                 command.getIdTransaccion(),
                 command.getCanal(),
                 command.getCodTipoConcepto(), 
                 command.getValMonto());
    }
    
    /**
     * Valida el request utilizando el validador de canal-concepto.
     */
    private void validateRequest(TransferCommand command) {
        channelConceptValidator.validate(command);
    }
    
    /**
     * Registra auditoría del request entrante.
     */
    private void auditEntryRequest(TransferCommand command) {
        auditPort.logAsync(AuditUtils.createEntradaLog(command, command));
    }
    
    /**
     * Enruta y procesa la transferencia según el concepto.
     * 
     * @param command el comando de transferencia
     * @return el resultado de la transferencia
     */
    private TransferResult routeAndProcessTransfer(TransferCommand command) {
        String concepto = command.getCodTipoConcepto();
        
        if (TransactionConstants.ConceptType.COBPER.equals(concepto)) {
            return processCoberPerConcept(command);
        } else {
            return processUnsupportedConcept(command, concepto);
        }
    }
    
    /**
     * Procesa el concepto COBPER (Cobro de Membresía) invocando PER001 en AS/400.
     * 
     * @param command el comando de transferencia
     * @return el resultado de la transferencia desde PER001
     */
    private TransferResult processCoberPerConcept(TransferCommand command) {
        LOG.info("Routing a PER001 (AS/400) para concepto COBPER");
        
        // Métrica: incrementar contador de llamadas PER001
        meterRegistry.counter("per001.calls", "concept", TransactionConstants.ConceptType.COBPER).increment();
        
        // AUDITORÍA TRAMA_OUT - Registrar invocación a PER001
        auditPort.logAsync(AuditUtils.createTramaOutLog(command, command));
        
        // Invocar programa RPG PER001
        TransferResult result = per001Service.processMembershipPayment(command);
        
        // AUDITORÍA TRAMA_IN - Registrar respuesta de PER001
        auditPort.logAsync(AuditUtils.createTramaInLog(command, result));
        
        return result;
    }
    
    /**
     * Procesa conceptos no soportados retornando una respuesta simulada.
     * 
     * @param command el comando de transferencia
     * @param concepto el tipo de concepto no soportado
     * @return un resultado simulado
     */
    private TransferResult processUnsupportedConcept(TransferCommand command, String concepto) {
        LOG.warn("Concepto {} no implementado, usando respuesta simulada", concepto);
        
        // Métrica: incrementar contador de conceptos no implementados
        meterRegistry.counter("transfer.simulated", "concept", concepto).increment();
        
        return buildSimulatedResult(command);
    }
    
    /**
     * Registra métricas de éxito de la transferencia.
     * 
     * @param concepto el tipo de concepto procesado
     * @param monto el monto de la transferencia
     */
    private void recordSuccessMetrics(String concepto, BigDecimal monto) {
        // Métrica: incrementar contador de transferencias exitosas por concepto
        meterRegistry.counter("transfer.success", "concept", concepto).increment();
        
        // Métrica: registrar monto de la transacción
        meterRegistry.summary("transfer.amount", "concept", concepto)
                .record(monto.doubleValue());
    }
    
    /**
     * Registra la finalización exitosa de la transferencia en los logs.
     */
    private void logTransferCompletion(TransferResult result) {
        LOG.info("Transferencia completada - comprobante={}", result.getValNumeroComprobante());
    }
    
    /**
     * Registra auditoría del response exitoso.
     */
    private void auditSuccessResponse(TransferCommand command, TransferResult result) {
        auditPort.logAsync(AuditUtils.createSalidaLog(command, result));
    }
    
    /**
     * Maneja errores durante el procesamiento de la transferencia.
     * Registra logs, métricas y auditoría de error.
     * 
     * @param command el comando de transferencia
     * @param e la excepción ocurrida
     */
    private void handleTransferError(TransferCommand command, Exception e) {
        LOG.error("Error en transferencia - idTransaccion={}", command.getIdTransaccion(), e);
        
        // Métrica: incrementar contador de errores por tipo de concepto
        String concepto = command.getCodTipoConcepto();
        meterRegistry.counter("transfer.error", 
                "concept", concepto,
                "exception", e.getClass().getSimpleName()).increment();
        
        // Auditoría: registrar excepción
        auditPort.logAsync(AuditUtils.createErrorLog(command, e, "OrqCompensacionUsecaseImpl.transfer"));
    }
    
    private TransferResult buildSimulatedResult(TransferCommand command) {
        TransferResult result = new TransferResult();
        
        // Header
        result.setNombreOperacion(command.getNombreOperacion());
        result.setTotal(command.getTotal());
        result.setCaracterAceptacion(TransactionConstants.AcceptanceCode.SUCCESS); // B = OK
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

