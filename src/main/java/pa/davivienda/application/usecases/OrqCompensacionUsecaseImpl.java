package pa.davivienda.application.usecases;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;
import pa.davivienda.domain.interfaces.usecases.OrqCompensacionService;

/**
 * Implementación del caso de uso de transferencia/compensación.
 * 
 * Esta clase contiene la lógica de negocio pura, independiente del protocolo REST.
 * Recibe TransferCommand y devuelve TransferResult (objetos del dominio).
 */
@ApplicationScoped
public class OrqCompensacionUsecaseImpl implements OrqCompensacionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(OrqCompensacionUsecaseImpl.class);
    
    @Override
    public TransferResult transfer(TransferCommand command) {
        LOG.info("Ejecutando transferencia - idTransaccion={}, concepto={}, monto={}", 
                 command.getIdTransaccion(), 
                 command.getCodTipoConcepto(), 
                 command.getValMonto());
        
        // TODO: Implementar lógica de negocio real
        // Por ahora, devolvemos un resultado simulado
        TransferResult result = new TransferResult();
        
        // Header
        result.setNombreOperacion(command.getNombreOperacion());
        result.setTotal(command.getTotal());
        result.setCaracterAceptacion("B"); // B = OK
        result.setUltimoMensaje((short) 0);
        result.setIdTransaccion(command.getIdTransaccion());
        result.setCodMsgRespuesta(0);
        result.setMsgRespuesta("Transacción exitosa");
        
        // Data
        result.setValNumeroComprobante("COMP-" + System.currentTimeMillis());
        result.setValSecuencial(System.currentTimeMillis());
        result.setFecHoraMovimiento(OffsetDateTime.now());
        result.setValMonto(command.getValMonto());
        result.setCostoDeLaTransaccion(BigDecimal.valueOf(2.50));
        result.setValTasaCambio(command.getValTasaCambio());
        result.setValMontoDestino(command.getValMontoDestino());
        result.setCodMonedaTransaccion(command.getCodMonedaDestino());
        
        LOG.info("Transferencia completada - comprobante={}", result.getValNumeroComprobante());
        
        return result;
    }
}

