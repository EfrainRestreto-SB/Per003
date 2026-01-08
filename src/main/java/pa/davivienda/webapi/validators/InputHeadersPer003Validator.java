package pa.davivienda.webapi.validators;

import pa.davivienda.webapi.dto.RequestHeaders;
import pa.davivienda.webapi.exceptions.BadRequestException;

/**
 * Validador personalizado para los headers HTTP del servicio PER003.
 * 
 * Valida que todos los headers requeridos estén presentes y tengan valores válidos
 * según las reglas de negocio específicas de la orquestación de compensaciones.
 */
public class InputHeadersPer003Validator {

    /**
     * Valida los headers HTTP recibidos en el request.
     * 
     * @param headers Headers HTTP encapsulados en RequestHeaders
     * @throws BadRequestException Si algún header es inválido o falta
     */
    public static void validateInputHeaders(RequestHeaders headers) {
        
        if (headers == null) {
            throw new BadRequestException("Los headers HTTP no pueden ser nulos");
        }
        
        // Validación de nombreOperacion
        if (headers.getNombreOperacion() == null || headers.getNombreOperacion().isBlank()) {
            throw new BadRequestException("Header 'nombreOperacion' es obligatorio y no puede estar vacío");
        }
        
        // Validación de total
        if (headers.getTotal() == null) {
            throw new BadRequestException("Header 'total' es obligatorio");
        }
        if (headers.getTotal() <= 0) {
            throw new BadRequestException("Header 'total' debe ser mayor a 0");
        }
        
        // Validación de jornada
        if (headers.getJornada() == null) {
            throw new BadRequestException("Header 'jornada' es obligatorio");
        }
        if (headers.getJornada() < 0) {
            throw new BadRequestException("Header 'jornada' debe ser un valor positivo");
        }
        
        // Validación de canal
        if (headers.getCanal() == null) {
            throw new BadRequestException("Header 'canal' es obligatorio");
        }
        if (headers.getCanal() < 0) {
            throw new BadRequestException("Header 'canal' debe ser un valor positivo");
        }
        
        // Validación de modoDeOperacion
        if (headers.getModoDeOperacion() == null) {
            throw new BadRequestException("Header 'modoDeOperacion' es obligatorio");
        }
        
        // Validación de usuario
        if (headers.getUsuario() == null || headers.getUsuario().isBlank()) {
            throw new BadRequestException("Header 'usuario' es obligatorio y no puede estar vacío");
        }
        if (headers.getUsuario().length() > 50) {
            throw new BadRequestException("Header 'usuario' no puede exceder 50 caracteres");
        }
        
        // Validación de perfil
        if (headers.getPerfil() == null) {
            throw new BadRequestException("Header 'perfil' es obligatorio");
        }
        
        // Validación de versionServicio
        if (headers.getVersionServicio() == null || headers.getVersionServicio().isBlank()) {
            throw new BadRequestException("Header 'versionServicio' es obligatorio y no puede estar vacío");
        }
        
        // Validación de idTransaccion (opcional - se puede generar si no viene)
        if (headers.getIdTransaccion() != null && !headers.getIdTransaccion().isBlank()) {
            if (headers.getIdTransaccion().length() > 100) {
                throw new BadRequestException("Header 'idTransaccion' no puede exceder 100 caracteres");
            }
        }
    }
    
    /**
     * Constructor privado para prevenir instanciación.
     * Esta es una clase utilitaria con métodos estáticos únicamente.
     */
    private InputHeadersPer003Validator() {
        throw new UnsupportedOperationException("Clase utilitaria no instanciable");
    }
}
