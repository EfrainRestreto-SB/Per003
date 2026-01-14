package pa.davivienda.domain.dtos.responses;

public class DataHeaderResponse {
    
    private String nombreOperacion;
    private Integer total;
    private String caracterAceptacion; // B = OK, M = Error
    private Short ultimoMensaje;
    private String idTransaccion;
    private Integer codMsgRespuesta;
    private String msgRespuesta;

    // Getters y Setters
    public String getNombreOperacion() { return nombreOperacion; }
    public void setNombreOperacion(String nombreOperacion) { this.nombreOperacion = nombreOperacion; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public String getCaracterAceptacion() { return caracterAceptacion; }
    public void setCaracterAceptacion(String caracterAceptacion) { this.caracterAceptacion = caracterAceptacion; }

    public Short getUltimoMensaje() { return ultimoMensaje; }
    public void setUltimoMensaje(Short ultimoMensaje) { this.ultimoMensaje = ultimoMensaje; }

    public String getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(String idTransaccion) { this.idTransaccion = idTransaccion; }

    public Integer getCodMsgRespuesta() { return codMsgRespuesta; }
    public void setCodMsgRespuesta(Integer codMsgRespuesta) { this.codMsgRespuesta = codMsgRespuesta; }

    public String getMsgRespuesta() { return msgRespuesta; }
    public void setMsgRespuesta(String msgRespuesta) { this.msgRespuesta = msgRespuesta; }
}
