package pa.davivienda.webapi.dto;

import org.jboss.resteasy.reactive.RestHeader;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Clase para capturar los headers HTTP del request.
 * Representa el DataHeader que viene en las cabeceras HTTP del request.
 * Usa @RestHeader de RESTEasy Reactive para mapear headers HTTP.
 */
public class RequestHeaders {

    @RestHeader("nombreOperacion")
    @NotBlank(message = "Header nombreOperacion es obligatorio")
    private String nombreOperacion;

    @RestHeader("total")
    @NotNull(message = "Header total es obligatorio")
    private Integer total;

    @RestHeader("jornada")
    @NotNull(message = "Header jornada es obligatorio")
    private Short jornada;

    @RestHeader("canal")
    @NotNull(message = "Header canal es obligatorio")
    private Short canal;

    @RestHeader("modoDeOperacion")
    @NotNull(message = "Header modoDeOperacion es obligatorio")
    private Short modoDeOperacion;

    @RestHeader("usuario")
    @NotBlank(message = "Header usuario es obligatorio")
    private String usuario;

    @RestHeader("perfil")
    @NotNull(message = "Header perfil es obligatorio")
    private Short perfil;

    @RestHeader("versionServicio")
    @NotBlank(message = "Header versionServicio es obligatorio")
    private String versionServicio;

    @RestHeader("idTransaccion")
    @NotBlank(message = "Header idTransaccion es obligatorio")
    private String idTransaccion;

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
}
