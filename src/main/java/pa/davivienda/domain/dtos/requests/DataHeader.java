package pa.davivienda.domain.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DataHeader {

    @NotBlank(message = "nombreOperacion es obligatorio")
    private String nombreOperacion;

    @NotNull(message = "total es obligatorio")
    private Integer total;

    @NotNull(message = "jornada es obligatorio")
    private Short jornada;

    @NotNull(message = "canal es obligatorio")
    private Short canal;

    @NotNull(message = "modoDeOperacion es obligatorio")
    private Short modoDeOperacion;

    @NotBlank(message = "usuario es obligatorio")
    private String usuario;

    @NotNull(message = "perfil es obligatorio")
    private Short perfil;

    @NotBlank(message = "versionServicio es obligatorio")
    private String versionServicio;

    @NotBlank(message = "idTransaccion es obligatorio")
    private String idTransaccion;

    // getters y setters
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
}

