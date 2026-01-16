package pa.davivienda.domain.ports.output;

import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;

/**
 * Port outbound para invocar el servicio externo PER001 (AS/400).
 * Este port define el contrato para comunicarse con el programa RPG PER001
 * que gestiona cobros de membresía (COBPER).
 */
public interface Per001ServicePort {
    
    /**
     * Procesa un cobro de membresía invocando el programa RPG PER001 en AS/400.
     * 
     * @param command Comando con los datos de la transferencia/cobro
     * @return Resultado de la operación procesada por PER001
     * @throws As400ConnectionException Si hay error de conexión con AS/400
     * @throws As400ProgramException Si el programa RPG retorna error
     */
    TransferResult processMembershipPayment(TransferCommand command);
}
