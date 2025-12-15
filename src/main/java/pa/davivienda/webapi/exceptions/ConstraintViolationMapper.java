package pa.davivienda.webapi.exceptions;



import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pa.davivienda.domain.dtos.responses.ErrorResponse;

@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        ErrorResponse err = ErrorResponse.fromMessage(400, "Errores de validación", null);
        exception.getConstraintViolations().forEach(v -> err.getDetalles().add(v.getPropertyPath() + ": " + v.getMessage()));
        return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
    }
}

