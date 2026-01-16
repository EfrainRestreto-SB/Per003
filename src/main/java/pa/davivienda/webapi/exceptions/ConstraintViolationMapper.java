package pa.davivienda.webapi.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pa.davivienda.domain.dtos.responses.ErrorResponse;

@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = LoggerFactory.getLogger(ConstraintViolationMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        LOG.error("Error de validación capturado:", exception);
        ErrorResponse err = ErrorResponse.fromMessage(400, "Errores de validación", null);
        exception.getConstraintViolations().forEach(v -> {
            String error = v.getPropertyPath() + ": " + v.getMessage();
            LOG.error("  - Validación fallida: {}", error);
            err.getDetalles().add(error);
        });
        return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
    }
}

