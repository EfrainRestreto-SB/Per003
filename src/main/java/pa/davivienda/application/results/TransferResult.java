package pa.davivienda.application.results;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Resultado de la ejecución de una transferencia/compensación.
 * Representa la respuesta del negocio independiente del protocolo REST.
 * Este objeto se usa en la capa de aplicación (application layer).
 */
public class TransferResult {

    // DataHeader
    private String nombreOperacion;
    private Integer total;
    private String caracterAceptacion;
    private Short ultimoMensaje;
    private String idTransaccion;
    private Integer codMsgRespuesta;
    private String msgRespuesta;

    // Data
    private String valNumeroComprobante;
    private Long valSecuencial;
    private OffsetDateTime fecHoraMovimiento;
    private BigDecimal valMonto;
    private BigDecimal costoDeLaTransaccion;
    private BigDecimal valTasaCambio;
    private BigDecimal valMontoDestino;
    private String codMonedaTransaccion;

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

    public String getValNumeroComprobante() { return valNumeroComprobante; }
    public void setValNumeroComprobante(String valNumeroComprobante) { this.valNumeroComprobante = valNumeroComprobante; }

    public Long getValSecuencial() { return valSecuencial; }
    public void setValSecuencial(Long valSecuencial) { this.valSecuencial = valSecuencial; }

    public OffsetDateTime getFecHoraMovimiento() { return fecHoraMovimiento; }
    public void setFecHoraMovimiento(OffsetDateTime fecHoraMovimiento) { this.fecHoraMovimiento = fecHoraMovimiento; }

    public BigDecimal getValMonto() { return valMonto; }
    public void setValMonto(BigDecimal valMonto) { this.valMonto = valMonto; }

    public BigDecimal getCostoDeLaTransaccion() { return costoDeLaTransaccion; }
    public void setCostoDeLaTransaccion(BigDecimal costoDeLaTransaccion) { this.costoDeLaTransaccion = costoDeLaTransaccion; }

    public BigDecimal getValTasaCambio() { return valTasaCambio; }
    public void setValTasaCambio(BigDecimal valTasaCambio) { this.valTasaCambio = valTasaCambio; }

    public BigDecimal getValMontoDestino() { return valMontoDestino; }
    public void setValMontoDestino(BigDecimal valMontoDestino) { this.valMontoDestino = valMontoDestino; }

    public String getCodMonedaTransaccion() { return codMonedaTransaccion; }
    public void setCodMonedaTransaccion(String codMonedaTransaccion) { this.codMonedaTransaccion = codMonedaTransaccion; }
}
