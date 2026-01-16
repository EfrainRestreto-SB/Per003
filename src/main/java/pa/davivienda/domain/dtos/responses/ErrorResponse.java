package pa.davivienda.domain.dtos.responses;

import java.util.ArrayList;
import java.util.List;
import pa.davivienda.transversal.constants.TransactionConstants;

public class ErrorResponse {
    private String caracterAceptacion;
    private int codMsgRespuesta;
    private String msgRespuesta;
    private String idTransaccion;
    private List<String> detalles = new ArrayList<>();

    public static ErrorResponse fromMessage(int code, String message, String idTransaccion) {
        ErrorResponse e = new ErrorResponse();
        e.setCaracterAceptacion(TransactionConstants.AcceptanceCode.ERROR);
        e.setCodMsgRespuesta(code);
        e.setMsgRespuesta(message);
        e.setIdTransaccion(idTransaccion);
        return e;
    }

    public static ErrorResponse fromValidation(Throwable ex, String idTransaccion) {
        ErrorResponse e = new ErrorResponse();
        e.setCaracterAceptacion(TransactionConstants.AcceptanceCode.ERROR);
        e.setCodMsgRespuesta(400);
        e.setMsgRespuesta("Error de validación");
        e.setIdTransaccion(idTransaccion);
        e.getDetalles().add(ex.getMessage());
        return e;
    }

    // getters / setters
    public String getCaracterAceptacion() { return caracterAceptacion; }
    public void setCaracterAceptacion(String caracterAceptacion) { this.caracterAceptacion = caracterAceptacion; }

    public int getCodMsgRespuesta() { return codMsgRespuesta; }
    public void setCodMsgRespuesta(int codMsgRespuesta) { this.codMsgRespuesta = codMsgRespuesta; }

    public String getMsgRespuesta() { return msgRespuesta; }
    public void setMsgRespuesta(String msgRespuesta) { this.msgRespuesta = msgRespuesta; }

    public String getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(String idTransaccion) { this.idTransaccion = idTransaccion; }

    public List<String> getDetalles() { return detalles; }
    public void setDetalles(List<String> detalles) { this.detalles = detalles; }
}
