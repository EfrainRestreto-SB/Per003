package pa.davivienda.domain.interfaces.usecases;

import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;

/**
 * Interface del servicio de orquestación de compensaciones.
 * Define el contrato de la lógica de negocio independiente del protocolo (hexagonal architecture).
 * 
 * Usa Commands y Results en lugar de DTOs REST para desacoplar la lógica de negocio
 * de los detalles de implementación de la API.
 */
public interface OrqCompensacionService {
    
    /**
     * Ejecuta una transferencia/compensación.
     * 
     * @param command Comando con todos los datos necesarios para ejecutar la transferencia
     * @return Resultado de la transferencia con los datos del comprobante
     */
    TransferResult transfer(TransferCommand command);
}


