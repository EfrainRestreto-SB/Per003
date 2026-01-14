package pa.davivienda.application.validators;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pa.davivienda.transversal.constants.TransactionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.domain.exceptions.InvalidChannelConceptException;
import pa.davivienda.domain.models.ConceptProgramConfig;
import pa.davivienda.transversal.config.ConceptProgramMappingConfig;

import java.util.Optional;

/**
 * Validador de combinaciones de canal y tipo de concepto.
 * 
 * <p>
 * Este validador implementa las reglas de negocio para determinar qué combinaciones
 * de canal y tipo de concepto son válidas según el Map de homologación en memoria.
 * </p>
 * 
 * <p>
 * <b>Funcionamiento:</b>
 * </p>
 * <ul>
 *   <li>Consulta el Map en memoria para validar el concepto</li>
 *   <li>Solo permite conceptos marcados como implementados</li>
 *   <li>Valida que el canal sea el correcto para cada concepto</li>
 *   <li>Rechaza conceptos no implementados o no configurados</li>
 * </ul>
 * 
 * <p>
 * <b>Configuraciones actuales en Map:</b>
 * </p>
 * <ul>
 *   <li>Canal 81 + COBPER - PER001 (Cobro de membresía) [IMPLEMENTADO]</li>
 *   <li>Otros canales: En desarrollo</li>
 * </ul>
 * 
 * @author Davivienda
 * @version 2.0
 * @since 1.0
 * @see TransferCommand
 * @see InvalidChannelConceptException
 * @see ConceptProgramConfig
 */
@ApplicationScoped
public class ChannelConceptValidator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelConceptValidator.class);

    @Inject
    ConceptProgramMappingConfig mappingConfig;
    
    /**
     * Valida que la combinación de canal y concepto sea permitida según la tabla de homologación.
     * 
     * <p>
     * El proceso de validación incluye:
     * <ol>
     *   <li>Verifica que canal y concepto no sean null</li>
     *   <li>Consulta la tabla de homologación para buscar el concepto</li>
     *   <li>Verifica que el concepto esté marcado como implementado</li>
     *   <li>Valida que el canal sea el correcto para ese concepto</li>
     * </ol>
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

        LOGGER.debug("Validando canal={} + concepto={}", canal, concepto);
        
        // Validar que solo canal 81 esté habilitado - otros canales en desarrollo
        if (canal != 81) {
            LOGGER.warn("Canal '{}' no está habilitado - En desarrollo", canal);
            throw new InvalidChannelConceptException(canal, concepto, "En desarrollo");
        }
        
        // Buscar en el Map en memoria si el concepto está implementado
        Optional<ConceptProgramConfig> mappingOpt = 
                mappingConfig.findImplementedConfig(concepto);
        
        if (mappingOpt.isEmpty()) {
            LOGGER.warn("Concepto '{}' no está implementado o no existe en el Map de configuración", concepto);
            throw new InvalidChannelConceptException(canal, concepto);
        }
        
        ConceptProgramConfig mapping = mappingOpt.get();
        LOGGER.debug("Concepto '{}' encontrado: programa={}, prfkey={}, descripcion={}", 
                concepto, mapping.getPrograma(), mapping.getCntrlprfPrfkey(), mapping.getDescripcion());
        
        // Validar que el canal sea el correcto para este concepto
        // Por ahora, validamos específicamente las combinaciones conocidas
        boolean isValidCombination = validateChannelForConcept(canal, concepto, mapping);
        
        if (!isValidCombination) {
            LOGGER.warn("Canal '{}' no es válido para concepto '{}'", canal, concepto);
            throw new InvalidChannelConceptException(canal, concepto,
                    String.format("Canal %d no es válido para el concepto %s. Programa: %s", 
                            canal, concepto, mapping.getPrograma()));
        }
        
        LOGGER.info("Validación exitosa: canal={} + concepto={} -> programa={}", 
                canal, concepto, mapping.getPrograma());
    }
    
    /**
     * Valida que el canal sea el correcto para el concepto especificado.
     * 
     * <p>Reglas de negocio:</p>
     * <ul>
     *   <li>Canal 81: Solo COBPER (PER001)</li>
     *   <li>Otros canales: En desarrollo</li>
     * </ul>
     * 
     * @param canal el código del canal
     * @param concepto el código del tipo de concepto
     * @param mapping el mapeo de homologación encontrado
     * @return true si la combinación es válida
     */
    private boolean validateChannelForConcept(Short canal, String concepto, ConceptProgramConfig mapping) {
        // Solo canal 81 habilitado - Solo COBPER
        if (canal == 81) {
            return TransactionConstants.ConceptType.COBPER.equalsIgnoreCase(concepto);
        }
        
        // Otros canales: En desarrollo
        LOGGER.warn("Canal '{}' no está habilitado - En desarrollo", canal);
        return false;
    }
    
    /**
     * Verifica si una combinación de canal y concepto es válida sin lanzar excepción.
     * Consulta el Map en memoria para determinar la validez.
     * Solo canal 81 está habilitado, otros canales retornan false (En desarrollo).
     * 
     * @param canal el código del canal
     * @param concepto el código del tipo de concepto
     * @return {@code true} si la combinación es válida, {@code false} en caso contrario
     */
    public boolean isValid(Short canal, String concepto) {
        if (canal == null || concepto == null || concepto.trim().isEmpty()) {
            return false;
        }
        
        // Solo canal 81 habilitado
        if (canal != 81) {
            LOGGER.debug("Canal '{}' no está habilitado - En desarrollo", canal);
            return false;
        }
        
        try {
            // Buscar en el Map en memoria
            Optional<ConceptProgramConfig> mappingOpt = 
                    mappingConfig.findImplementedConfig(concepto);
            
            if (mappingOpt.isEmpty()) {
                return false;
            }
            
            // Validar canal para el concepto
            return validateChannelForConcept(canal, concepto, mappingOpt.get());
            
        } catch (Exception e) {
            LOGGER.error("Error validando canal={} + concepto={}", canal, concepto, e);
            return false;
        }
    }
    
    /**
     * Obtiene el programa AS/400 que debe ejecutarse para un concepto dado.
     * 
     * @param concepto el código del tipo de concepto
     * @return Optional con el nombre del programa si el concepto está implementado
     */
    public Optional<String> getProgramForConcept(String concepto) {
        if (concepto == null || concepto.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return mappingConfig.getProgramForConcept(concepto);
    }
}
