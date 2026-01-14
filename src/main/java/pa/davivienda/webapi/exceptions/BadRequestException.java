package pa.davivienda.webapi.exceptions;

/**
 * Excepción para errores de validación de peticiones HTTP.
 * Se lanza cuando los datos de entrada no cumplen con los requisitos esperados.
 */
public class BadRequestException extends RuntimeException {
    
    private final int statusCode;
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message Mensaje descriptivo del error de validación
     */
    public BadRequestException(String message) {
        super(message);
        this.statusCode = 400;
    }
    
    /**
     * Constructor con mensaje y causa.
     * 
     * @param message Mensaje descriptivo del error
     * @param cause Causa raíz de la excepción
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 400;
    }
    
    /**
     * Obtiene el código de estado HTTP asociado.
     * 
     * @return Código 400 (Bad Request)
     */
    public int getStatusCode() {
        return statusCode;
    }
}
