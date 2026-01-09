package pa.davivienda.application.validators;

import jakarta.enterprise.context.ApplicationScoped;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.domain.exceptions.InvalidChannelConceptException;

/**
 * Validador de combinaciones de canal y tipo de concepto.
 * 
 * <p>
 * Este validador implementa las reglas de negocio para determinar qué combinaciones
 * de canal y tipo de concepto son válidas en el entorno de desarrollo.
 * </p>
 * 
 * <p>
 * <b>Reglas de validación en desarrollo:</b>
 * </p>
 * <ul>
 *   <li><b>Canal 81 + COBPER:</b> ✅ Válido → Enruta a PER001 (AS/400)</li>
 *   <li><b>Cualquier otra combinación:</b> ❌ Rechazado → Lanza {@link InvalidChannelConceptException}</li>
 * </ul>
 * 
 * <p>
 * <b>Reglas futuras (producción):</b>
 * </p>
 * <ul>
 *   <li>Canal 81 + COBPER → PER001 (Cobro de membresía)</li>
 *   <li>Canal 151 + TRCPRO → PER004 (Transferencias regionales cuentas propias)</li>
 *   <li>Canal 151 + TRCTER → PER005 (Transferencias regionales a terceros)</li>
 *   <li>Canal 151 + TININD → PER006 (Transferencias internacionales individuales)</li>
 * </ul>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 * @see TransferCommand
 * @see InvalidChannelConceptException
 */
@ApplicationScoped
public class ChannelConceptValidator {
    
    /**
     * Valida que la combinación de canal y concepto sea permitida en desarrollo.
     * 
     * <p>
     * En el entorno de desarrollo actual, solo se permite:
     * <ul>
     *   <li><b>Canal 81 + COBPER</b></li>
     * </ul>
     * </p>
     * 
     * <p>
     * Cualquier otra combinación lanzará {@link InvalidChannelConceptException}.
     * </p>
     * 
     * @param command el comando de transferencia a validar, no debe ser {@code null}
     * @throws InvalidChannelConceptException si la combinación canal-concepto no es válida
     * @throws IllegalArgumentException si el comando es {@code null}
     */
    public void validate(TransferCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("TransferCommand no puede ser null");
        }
        
        Short canal = command.getCanal();
        String concepto = command.getCodTipoConcepto();
        
        // Validar que canal y concepto no sean null
        if (canal == null) {
            throw new InvalidChannelConceptException(null, concepto, 
                    "El canal no puede ser null");
        }
        
        if (concepto == null || concepto.trim().isEmpty()) {
            throw new InvalidChannelConceptException(canal, concepto, 
                    "El tipo de concepto no puede ser null o vacío");
        }
        
        // Validar combinación canal + concepto (solo 81 + COBPER está implementado)
        if (canal.shortValue() == 81 && "COBPER".equalsIgnoreCase(concepto)) {
            // ✅ Combinación válida: Canal 81 + COBPER → PER001 implementado
            return;
        }
        
        // ❌ Otras operaciones no están implementadas todavía
        throw new InvalidChannelConceptException(canal, concepto);
    }
    
    /**
     * Verifica si una combinación de canal y concepto es válida sin lanzar excepción.
     * 
     * @param canal el código del canal
     * @param concepto el código del tipo de concepto
     * @return {@code true} si la combinación es válida, {@code false} en caso contrario
     */
    public boolean isValid(Short canal, String concepto) {
        if (canal == null || concepto == null || concepto.trim().isEmpty()) {
            return false;
        }
        
        // En desarrollo, solo canal 81 + COBPER
        return canal.shortValue() == 81 && "COBPER".equalsIgnoreCase(concepto);
    }
}
