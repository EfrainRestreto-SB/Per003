package pa.davivienda.webapi.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pa.davivienda.domain.dtos.responses.ErrorResponse;
import pa.davivienda.domain.exceptions.InvalidChannelConceptException;

/**
 * Exception mapper para manejar {@link InvalidChannelConceptException}.
 * 
 * <p>
 * Este mapper intercepta las excepciones de combinación canal-concepto inválida
 * y las convierte en respuestas HTTP 400 (Bad Request) con un formato estándar.
 * </p>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 */
@Provider
public class InvalidChannelConceptExceptionMapper implements ExceptionMapper<InvalidChannelConceptException> {
    
    private static final Logger LOG = LoggerFactory.getLogger(InvalidChannelConceptExceptionMapper.class);
    
    @Override
    public Response toResponse(InvalidChannelConceptException exception) {
        LOG.error("Combinación canal-concepto inválida: canal={}, concepto={}", 
                 exception.getCanal(), 
                 exception.getConcepto());
        
        // Crear respuesta de error con formato estándar
        ErrorResponse response = new ErrorResponse();
        response.setCaracterAceptacion("N"); // N = Error
        response.setCodMsgRespuesta(40001); // Código de error personalizado
        response.setMsgRespuesta("Funcionalidad no implementada");
        response.getDetalles().add(exception.getMessage());
        response.getDetalles().add(String.format("Canal recibido: %s", exception.getCanal()));
        response.getDetalles().add(String.format("Concepto recibido: %s", exception.getConcepto()));
        response.getDetalles().add("Actualmente solo está implementado: COBPER (canal 81)");
        response.getDetalles().add("Otros tipos de operación estarán disponibles en futuras versiones");
        
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }
}
