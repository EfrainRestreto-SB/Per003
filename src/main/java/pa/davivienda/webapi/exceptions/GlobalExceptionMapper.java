package pa.davivienda.webapi.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pa.davivienda.domain.dtos.responses.ErrorResponse;

/**
 * Mapper global para capturar TODAS las excepciones no manejadas.
 * Útil para debugging y logging centralizado.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        // Si es WebApplicationException (como 404, 405, etc), preservar el status
        if (exception instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) exception;
            int status = wae.getResponse().getStatus();
            
            LOG.error("🔴 WebApplicationException capturada - Status {}: {}", status, exception.getMessage(), exception);
            
            ErrorResponse err = ErrorResponse.fromMessage(
                status, 
                "Error en la petición: " + exception.getMessage(), 
                null
            );
            
            return Response.status(status).entity(err).build();
        }
        
        // Cualquier otra excepción
        LOG.error("🔴 Excepción no manejada capturada:", exception);
        
        ErrorResponse err = ErrorResponse.fromMessage(
            500, 
            "Error interno del servidor: " + exception.getMessage(), 
            null
        );
        
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(err).build();
    }
}
