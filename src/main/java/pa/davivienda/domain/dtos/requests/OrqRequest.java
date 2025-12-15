package pa.davivienda.domain.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrqRequest {

    @JsonProperty("NombredelServicio")
    @NotBlank(message = "NombredelServicio es obligatorio")
    private String nombredelServicio;

    @Valid
    @NotNull(message = "DataHeader es obligatorio")
    private DataHeader dataHeader;

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

    public DataHeader getDataHeader() {
        return dataHeader;
    }

    public void setDataHeader(DataHeader dataHeader) {
        this.dataHeader = dataHeader;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}

