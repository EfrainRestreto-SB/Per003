package pa.davivienda.domain.dtos.requests;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class Data {

    // Campos opcionales de sesión y contexto
    private String idSesion;
    private String codIdioma;
    private String valOrigen; // IP origen
    private String codPais;
    private String valVersionApp;

    // Identificación del cliente
    @NotBlank(message = "codTipoIdentificacion es obligatorio")
    private String codTipoIdentificacion;

    @NotBlank(message = "valNumeroIdentificacion es obligatorio")
    private String valNumeroIdentificacion;

    // Producto
    @NotBlank(message = "codTipoProducto es obligatorio")
    private String codTipoProducto;

    @NotBlank(message = "valNumeroProducto es obligatorio")
    private String valNumeroProducto;

    @NotBlank(message = "codMonedaProducto es obligatorio")
    private String codMonedaProducto;

    // Movimiento
    @NotBlank(message = "codTipoMovimiento es obligatorio")
    private String codTipoMovimiento;

    private String valNumeroContrato; // Deal ticket para tipo de cambio especial

    @NotNull(message = "valMonto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "valMonto debe ser mayor que 0")
    private BigDecimal valMonto;

    private BigDecimal valTasaCambio;
    private BigDecimal valMontoDestino;

    @NotBlank(message = "codMonedaDestino es obligatorio")
    private String codMonedaDestino;

    // Concepto y descripción
    @NotBlank(message = "codTipoConcepto es obligatorio")
    private String codTipoConcepto; // COBPER, TRCPRO, TRCTER, TININD, etc.

    private String codTipoMotivo; // Solo para SV

    @NotBlank(message = "valDescripcion es obligatorio")
    private String valDescripcion;

    // Campos específicos de SV
    private String codParentescoBeneficiario;
    private String flagGrupoEmpresarialBeneficiario;
    private String valTelefonoBeneficiario;

    // Beneficiario (obligatorios para PA)
    private String valNombreBeneficiario;
    private String valNumeroCuentaBeneficiario;

    // País origen y destino
    private String codPaisOrigen;
    private String codPaisDestino;

    // Reversa
    private Boolean flagReversa;
    private Long valSecReverso;

    // Getters y Setters
    public String getIdSesion() { return idSesion; }
    public void setIdSesion(String idSesion) { this.idSesion = idSesion; }

    public String getCodIdioma() { return codIdioma; }
    public void setCodIdioma(String codIdioma) { this.codIdioma = codIdioma; }

    public String getValOrigen() { return valOrigen; }
    public void setValOrigen(String valOrigen) { this.valOrigen = valOrigen; }

    public String getCodPais() { return codPais; }
    public void setCodPais(String codPais) { this.codPais = codPais; }

    public String getValVersionApp() { return valVersionApp; }
    public void setValVersionApp(String valVersionApp) { this.valVersionApp = valVersionApp; }

    public String getCodTipoIdentificacion() { return codTipoIdentificacion; }
    public void setCodTipoIdentificacion(String codTipoIdentificacion) { this.codTipoIdentificacion = codTipoIdentificacion; }

    public String getValNumeroIdentificacion() { return valNumeroIdentificacion; }
    public void setValNumeroIdentificacion(String valNumeroIdentificacion) { this.valNumeroIdentificacion = valNumeroIdentificacion; }

    public String getCodTipoProducto() { return codTipoProducto; }
    public void setCodTipoProducto(String codTipoProducto) { this.codTipoProducto = codTipoProducto; }

    public String getValNumeroProducto() { return valNumeroProducto; }
    public void setValNumeroProducto(String valNumeroProducto) { this.valNumeroProducto = valNumeroProducto; }

    public String getCodMonedaProducto() { return codMonedaProducto; }
    public void setCodMonedaProducto(String codMonedaProducto) { this.codMonedaProducto = codMonedaProducto; }

    public String getCodTipoMovimiento() { return codTipoMovimiento; }
    public void setCodTipoMovimiento(String codTipoMovimiento) { this.codTipoMovimiento = codTipoMovimiento; }

    public String getValNumeroContrato() { return valNumeroContrato; }
    public void setValNumeroContrato(String valNumeroContrato) { this.valNumeroContrato = valNumeroContrato; }

    public BigDecimal getValMonto() { return valMonto; }
    public void setValMonto(BigDecimal valMonto) { this.valMonto = valMonto; }

    public BigDecimal getValTasaCambio() { return valTasaCambio; }
    public void setValTasaCambio(BigDecimal valTasaCambio) { this.valTasaCambio = valTasaCambio; }

    public BigDecimal getValMontoDestino() { return valMontoDestino; }
    public void setValMontoDestino(BigDecimal valMontoDestino) { this.valMontoDestino = valMontoDestino; }

    public String getCodMonedaDestino() { return codMonedaDestino; }
    public void setCodMonedaDestino(String codMonedaDestino) { this.codMonedaDestino = codMonedaDestino; }

    public String getCodTipoConcepto() { return codTipoConcepto; }
    public void setCodTipoConcepto(String codTipoConcepto) { this.codTipoConcepto = codTipoConcepto; }

    public String getCodTipoMotivo() { return codTipoMotivo; }
    public void setCodTipoMotivo(String codTipoMotivo) { this.codTipoMotivo = codTipoMotivo; }

    public String getValDescripcion() { return valDescripcion; }
    public void setValDescripcion(String valDescripcion) { this.valDescripcion = valDescripcion; }

    public String getCodParentescoBeneficiario() { return codParentescoBeneficiario; }
    public void setCodParentescoBeneficiario(String codParentescoBeneficiario) { this.codParentescoBeneficiario = codParentescoBeneficiario; }

    public String getFlagGrupoEmpresarialBeneficiario() { return flagGrupoEmpresarialBeneficiario; }
    public void setFlagGrupoEmpresarialBeneficiario(String flagGrupoEmpresarialBeneficiario) { this.flagGrupoEmpresarialBeneficiario = flagGrupoEmpresarialBeneficiario; }

    public String getValTelefonoBeneficiario() { return valTelefonoBeneficiario; }
    public void setValTelefonoBeneficiario(String valTelefonoBeneficiario) { this.valTelefonoBeneficiario = valTelefonoBeneficiario; }

    public String getValNombreBeneficiario() { return valNombreBeneficiario; }
    public void setValNombreBeneficiario(String valNombreBeneficiario) { this.valNombreBeneficiario = valNombreBeneficiario; }

    public String getValNumeroCuentaBeneficiario() { return valNumeroCuentaBeneficiario; }
    public void setValNumeroCuentaBeneficiario(String valNumeroCuentaBeneficiario) { this.valNumeroCuentaBeneficiario = valNumeroCuentaBeneficiario; }

    public String getCodPaisOrigen() { return codPaisOrigen; }
    public void setCodPaisOrigen(String codPaisOrigen) { this.codPaisOrigen = codPaisOrigen; }

    public String getCodPaisDestino() { return codPaisDestino; }
    public void setCodPaisDestino(String codPaisDestino) { this.codPaisDestino = codPaisDestino; }

    public Boolean getFlagReversa() { return flagReversa; }
    public void setFlagReversa(Boolean flagReversa) { this.flagReversa = flagReversa; }

    public Long getValSecReverso() { return valSecReverso; }
    public void setValSecReverso(Long valSecReverso) { this.valSecReverso = valSecReverso; }
}
