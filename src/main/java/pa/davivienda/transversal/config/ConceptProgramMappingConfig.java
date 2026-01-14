package pa.davivienda.transversal.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pa.davivienda.domain.models.ConceptProgramConfig;
import pa.davivienda.transversal.constants.TransactionConstants;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuración de homologación entre tipos de concepto y programas AS/400.
 * Mantiene un Map en memoria con las definiciones de la tabla del requerimiento.
 * 
 * <p>Este Map se inicializa al arrancar la aplicación y contiene:
 * - COBPER → PER001 (implementado)
 * - TRCPRO → PER004 (pendiente)
 * - TRCTER → PER005 (pendiente)
 * - TININD, TINARC, TRA11R, TR1VR, PPRREG (pendientes)
 * 
 * <p>Solo los conceptos marcados como implementados pueden ser procesados.
 */
@ApplicationScoped
public class ConceptProgramMappingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConceptProgramMappingConfig.class);

    /**
     * Map principal: CodTipoConcepto → List<ConceptProgramConfig>
     * Usa List porque algunos conceptos tienen múltiples configuraciones (diferentes prfkey)
     */
    private Map<String, List<ConceptProgramConfig>> conceptProgramMap;

    @PostConstruct
    public void init() {
        LOGGER.info("Inicializando Map de homologación concepto-programa");
        conceptProgramMap = new HashMap<>();
        loadMappings();
        LOGGER.info("Map de homologación inicializado con {} conceptos", conceptProgramMap.size());
    }

    /**
     * Carga las definiciones de homologación según la tabla del requerimiento.
     */
    private void loadMappings() {
        // COBPER - Cobro de Membresía (IMPLEMENTADO)
        addMapping(TransactionConstants.ConceptType.COBPER, "01PAR157", "Cobro de Membresía", "PER001", true);

        // TRCPRO - Transferencias a cuentas propias (PENDIENTE)
        addMapping("TRCPRO", "01PAR153", "Trx a cuentas propias", "PER004", false);

        // TRCTER - Transferencias a Terceros (PENDIENTE)
        addMapping("TRCTER", "01PAR154", "Trx a Terceros", "PER005", false);

        // TRA11R - Configuración múltiple (PENDIENTE)
        addMapping("TRA11R", "01PAR153", "TRA11R - Config 1", "PER004", false);
        addMapping("TRA11R", "01PAR154", "TRA11R - Config 2", "PER005", false);

        // TR1VR - Configuración múltiple (PENDIENTE)
        addMapping("TR1VR", "01PAR153", "TR1VR - Config 1", "PER004", false);
        addMapping("TR1VR", "01PAR154", "TR1VR - Config 2", "PER005", false);

        // TININD - Transferencias Internacionales Individuales (PENDIENTE)
        addMapping("TININD", "01PAR150", "TININD - Config 1", "PER006", false);
        addMapping("TININD", "01PAR151", "TININD - Config 2", "PER006", false);
        addMapping("TININD", "01PAR152", "TININD - Config 3", "PER006", false);

        // TINARC - Transferencias Internacionales (PENDIENTE)
        addMapping("TINARC", "01PAR150", "TINARC - Config 1", "PER006", false);
        addMapping("TINARC", "01PAR151", "TINARC - Config 2", "PER006", false);
        addMapping("TINARC", "01PAR152", "TINARC - Config 3", "PER006", false);

        // PPRREG - Por definir (PENDIENTE)
        addMapping("PPRREG", "Por definir", "PPRREG - Por definir", "POR_DEFINIR", false);
    }

    /**
     * Agrega una configuración al Map.
     */
    private void addMapping(String codTipoConcepto, String cntrlprfPrfkey, 
                           String descripcion, String programa, boolean implementado) {
        ConceptProgramConfig config = new ConceptProgramConfig(
                codTipoConcepto, cntrlprfPrfkey, descripcion, programa, implementado);
        
        conceptProgramMap
                .computeIfAbsent(codTipoConcepto.toUpperCase(), k -> new ArrayList<>())
                .add(config);
        
        LOGGER.debug("Agregado mapeo: {} -> {} (implementado={})", 
                codTipoConcepto, programa, implementado);
    }

    /**
     * Busca la configuración de un concepto que esté implementado.
     * Si el concepto tiene múltiples configuraciones, retorna la primera implementada.
     * 
     * @param codTipoConcepto el código del tipo de concepto
     * @return Optional con la configuración si existe y está implementada
     */
    public Optional<ConceptProgramConfig> findImplementedConfig(String codTipoConcepto) {
        if (codTipoConcepto == null || codTipoConcepto.trim().isEmpty()) {
            return Optional.empty();
        }

        List<ConceptProgramConfig> configs = conceptProgramMap.get(codTipoConcepto.toUpperCase());
        if (configs == null || configs.isEmpty()) {
            return Optional.empty();
        }

        return configs.stream()
                .filter(ConceptProgramConfig::isImplementado)
                .findFirst();
    }

    /**
     * Busca la configuración por concepto y prfkey específicos.
     * 
     * @param codTipoConcepto el código del tipo de concepto
     * @param cntrlprfPrfkey la clave de perfil
     * @return Optional con la configuración si existe
     */
    public Optional<ConceptProgramConfig> findByConceptAndPrfkey(String codTipoConcepto, String cntrlprfPrfkey) {
        if (codTipoConcepto == null || cntrlprfPrfkey == null) {
            return Optional.empty();
        }

        List<ConceptProgramConfig> configs = conceptProgramMap.get(codTipoConcepto.toUpperCase());
        if (configs == null || configs.isEmpty()) {
            return Optional.empty();
        }

        return configs.stream()
                .filter(c -> c.getCntrlprfPrfkey().equalsIgnoreCase(cntrlprfPrfkey))
                .findFirst();
    }

    /**
     * Verifica si un concepto está implementado.
     * 
     * @param codTipoConcepto el código del tipo de concepto
     * @return true si existe al menos una configuración implementada
     */
    public boolean isImplemented(String codTipoConcepto) {
        return findImplementedConfig(codTipoConcepto).isPresent();
    }

    /**
     * Obtiene el programa AS/400 para un concepto implementado.
     * 
     * @param codTipoConcepto el código del tipo de concepto
     * @return Optional con el nombre del programa
     */
    public Optional<String> getProgramForConcept(String codTipoConcepto) {
        return findImplementedConfig(codTipoConcepto)
                .map(ConceptProgramConfig::getPrograma);
    }

    /**
     * Lista todos los conceptos implementados.
     * 
     * @return Lista de códigos de concepto implementados
     */
    public List<String> getImplementedConcepts() {
        return conceptProgramMap.values().stream()
                .flatMap(List::stream)
                .filter(ConceptProgramConfig::isImplementado)
                .map(ConceptProgramConfig::getCodTipoConcepto)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Lista todas las configuraciones de un concepto (implementadas y no implementadas).
     * 
     * @param codTipoConcepto el código del tipo de concepto
     * @return Lista de configuraciones
     */
    public List<ConceptProgramConfig> getAllConfigsForConcept(String codTipoConcepto) {
        if (codTipoConcepto == null) {
            return Collections.emptyList();
        }
        return conceptProgramMap.getOrDefault(codTipoConcepto.toUpperCase(), Collections.emptyList());
    }

    /**
     * Retorna el Map completo (solo lectura).
     * 
     * @return Map inmutable con todas las configuraciones
     */
    public Map<String, List<ConceptProgramConfig>> getAllMappings() {
        return Collections.unmodifiableMap(conceptProgramMap);
    }
}
