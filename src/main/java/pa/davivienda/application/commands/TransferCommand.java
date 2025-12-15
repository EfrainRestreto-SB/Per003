package pa.davivienda.application.commands;

import java.math.BigDecimal;

/**
 * Comando para ejecutar una transferencia/compensación.
 * Representa la intención del negocio independiente del protocolo REST.
 * Este objeto se usa en la capa de aplicación (application layer).
 */
public class TransferCommand {

    // Header
    private String nombreOperacion;
    private Integer total;
    private Short jornada;
    private Short canal;
    private Short modoDeOperacion;
    private String usuario;
    private Short perfil;
    private String versionServicio;
    private String idTransaccion;

    // Data - Sesión y contexto
    private String idSesion;
    private String codIdioma;
    private String valOrigen;
    private String codPais;
    private String valVersionApp;

    // Data - Identificación
    private String codTipoIdentificacion;
    private String valNumeroIdentificacion;

    // Data - Producto
    private String codTipoProducto;
    private String valNumeroProducto;
    private String codMonedaProducto;

    // Data - Movimiento
    private String codTipoMovimiento;
    private String valNumeroContrato;
    private BigDecimal valMonto;
    private BigDecimal valTasaCambio;
    private BigDecimal valMontoDestino;
    private String codMonedaDestino;

    // Data - Concepto
    private String codTipoConcepto;
    private String codTipoMotivo;
    private String valDescripcion;

    // Data - Campos específicos SV
    private String codParentescoBeneficiario;
    private String flagGrupoEmpresarialBeneficiario;
    private String valTelefonoBeneficiario;

    // Data - Beneficiario (PA)
    private String valNombreBeneficiario;
    private String valNumeroCuentaBeneficiario;

    // Data - País
    private String codPaisOrigen;
    private String codPaisDestino;

    // Data - Reversa
    private Boolean flagReversa;
    private Long valSecReverso;

    // Getters y Setters
    public String getNombreOperacion() { return nombreOperacion; }
    public void setNombreOperacion(String nombreOperacion) { this.nombreOperacion = nombreOperacion; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public Short getJornada() { return jornada; }
    public void setJornada(Short jornada) { this.jornada = jornada; }

    public Short getCanal() { return canal; }
    public void setCanal(Short canal) { this.canal = canal; }

    public Short getModoDeOperacion() { return modoDeOperacion; }
    public void setModoDeOperacion(Short modoDeOperacion) { this.modoDeOperacion = modoDeOperacion; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public Short getPerfil() { return perfil; }
    public void setPerfil(Short perfil) { this.perfil = perfil; }

    public String getVersionServicio() { return versionServicio; }
    public void setVersionServicio(String versionServicio) { this.versionServicio = versionServicio; }

    public String getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(String idTransaccion) { this.idTransaccion = idTransaccion; }

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
