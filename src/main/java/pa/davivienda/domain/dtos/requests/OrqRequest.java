package pa.davivienda.domain.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO REST para el body del request.
 * DataHeader ahora viene en HTTP headers, no en el body.
 */
public class OrqRequest {

    @JsonProperty("NombredelServicio")
    @NotBlank(message = "NombredelServicio es obligatorio")
    private String nombredelServicio;

    @JsonProperty("Data")
    @Valid
    @NotNull(message = "Data es obligatorio")
    private Data data;

    // getters y setters

    public String getNombredelServicio() {
        return nombredelServicio;
    }

    public void setNombredelServicio(String nombredelServicio) {
        this.nombredelServicio = nombredelServicio;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}

