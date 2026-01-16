package pa.davivienda.domain.exceptions;

/**
 * Excepción lanzada cuando la combinación de canal y concepto no es válida.
 * 
 * <p>
 * Esta excepción se utiliza para indicar que la combinación específica de
 * canal y tipo de concepto no está permitida o no está implementada en el sistema.
 * </p>
 * 
 * <p>
 * Reglas de validación actuales (desarrollo):
 * <ul>
 *   <li><b>Canal 81 + COBPER:</b> Válido (PER001 - AS/400)</li>
 *   <li><b>Cualquier otra combinación:</b> Inválido (no implementado)</li>
 * </ul>
 * </p>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 */
public class InvalidChannelConceptException extends RuntimeException {
    
    private final Short canal;
    private final String concepto;
    
    /**
     * Construye una nueva excepción con el canal y concepto inválidos.
     * 
     * @param canal el código del canal que fue rechazado
     * @param concepto el código del tipo de concepto que fue rechazado
     */
    public InvalidChannelConceptException(Short canal, String concepto) {
        super(String.format("Funcionalidad no implementada para canal=%s y concepto=%s. " +
                           "Actualmente solo está disponible COBPER (canal 81)", 
                           canal, concepto));
        this.canal = canal;
        this.concepto = concepto;
    }
    
    /**
     * Construye una nueva excepción con un mensaje personalizado.
     * 
     * @param canal el código del canal que fue rechazado
     * @param concepto el código del tipo de concepto que fue rechazado
     * @param message el mensaje de error personalizado
     */
    public InvalidChannelConceptException(Short canal, String concepto, String message) {
        super(message);
        this.canal = canal;
        this.concepto = concepto;
    }
    
    /**
     * Obtiene el código del canal inválido.
     * 
     * @return el código del canal
     */
    public Short getCanal() {
        return canal;
    }
    
    /**
     * Obtiene el código del concepto inválido.
     * 
     * @return el código del tipo de concepto
     */
    public String getConcepto() {
        return concepto;
    }
}
