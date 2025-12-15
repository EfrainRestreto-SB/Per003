package pa.davivienda.domain.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrqResponse {

    @JsonProperty("NombredelServicioResponse")
    private String nombredelServicioResponse;
    
    @JsonProperty("DataHeader")
    private DataHeaderResponse dataHeader;
    
    @JsonProperty("Data")
    private DataResponse data;

    // Getters y Setters
    public String getNombredelServicioResponse() { return nombredelServicioResponse; }
    public void setNombredelServicioResponse(String nombredelServicioResponse) { this.nombredelServicioResponse = nombredelServicioResponse; }

    public DataHeaderResponse getDataHeader() { return dataHeader; }
    public void setDataHeader(DataHeaderResponse dataHeader) { this.dataHeader = dataHeader; }

    public DataResponse getData() { return data; }
    public void setData(DataResponse data) { this.data = data; }
}

