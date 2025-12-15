package pa.davivienda.domain.dtos.responses;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class DataResponse {
    
    private String valNumeroComprobante;
    private Long valSecuencial;
    private OffsetDateTime fecHoraMovimiento; // Formato UTC: yyyy-mm-ddThh:mm:ss-06:00
    private BigDecimal valMonto;
    private BigDecimal costoDeLaTransaccion;
    private BigDecimal valTasaCambio;
    private BigDecimal valMontoDestino;
    private String codMonedaTransaccion; // Solo para PA

    // Getters y Setters
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

