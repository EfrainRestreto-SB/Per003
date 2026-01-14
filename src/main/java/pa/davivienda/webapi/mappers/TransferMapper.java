package pa.davivienda.webapi.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;
import pa.davivienda.domain.dtos.requests.OrqRequest;
import pa.davivienda.domain.dtos.responses.DataHeaderResponse;
import pa.davivienda.domain.dtos.responses.DataResponse;
import pa.davivienda.domain.dtos.responses.OrqResponse;
import pa.davivienda.webapi.dto.RequestHeaders;

/**
 * Mapper MapStruct para convertir entre DTOs REST y Commands/Results del application layer.
 * 
 * Separación de responsabilidades (Hexagonal Architecture):
 * - OrqRequest/OrqResponse: DTOs de la capa de API (webapi) - acoplados al contrato REST
 * - RequestHeaders: Captura headers HTTP del request
 * - TransferCommand/TransferResult: Objetos de la capa de aplicación - independientes del protocolo
 * 
 * Este mapper actúa como un adaptador entre la capa de presentación (REST) y la capa de aplicación.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface TransferMapper {

    /**
     * Convierte headers HTTP + body JSON a TransferCommand (comando de aplicación).
     * 
     * Mapea los campos desde:
     * - RequestHeaders (HTTP headers): nombreOperacion, total, jornada, canal, etc.
     * - OrqRequest.Data (body JSON): datos de negocio de la transferencia
     * 
     * hacia un objeto plano TransferCommand para que la lógica de negocio 
     * no dependa de la estructura REST.
     */
    @Mapping(source = "headers.nombreOperacion", target = "nombreOperacion")
    @Mapping(source = "headers.total", target = "total")
    @Mapping(source = "headers.jornada", target = "jornada")
    @Mapping(source = "headers.canal", target = "canal")
    @Mapping(source = "headers.modoDeOperacion", target = "modoDeOperacion")
    @Mapping(source = "headers.usuario", target = "usuario")
    @Mapping(source = "headers.perfil", target = "perfil")
    @Mapping(source = "headers.versionServicio", target = "versionServicio")
    @Mapping(source = "headers.idTransaccion", target = "idTransaccion")
    @Mapping(source = "request.data.idSesion", target = "idSesion")
    @Mapping(source = "request.data.codIdioma", target = "codIdioma")
    @Mapping(source = "request.data.valOrigen", target = "valOrigen")
    @Mapping(source = "request.data.codPais", target = "codPais")
    @Mapping(source = "request.data.valVersionApp", target = "valVersionApp")
    @Mapping(source = "request.data.codTipoIdentificacion", target = "codTipoIdentificacion")
    @Mapping(source = "request.data.valNumeroIdentificacion", target = "valNumeroIdentificacion")
    @Mapping(source = "request.data.codTipoProducto", target = "codTipoProducto")
    @Mapping(source = "request.data.valNumeroProducto", target = "valNumeroProducto")
    @Mapping(source = "request.data.codMonedaProducto", target = "codMonedaProducto")
    @Mapping(source = "request.data.codTipoMovimiento", target = "codTipoMovimiento")
    @Mapping(source = "request.data.valNumeroContrato", target = "valNumeroContrato")
    @Mapping(source = "request.data.valMonto", target = "valMonto")
    @Mapping(source = "request.data.valTasaCambio", target = "valTasaCambio")
    @Mapping(source = "request.data.valMontoDestino", target = "valMontoDestino")
    @Mapping(source = "request.data.codMonedaDestino", target = "codMonedaDestino")
    @Mapping(source = "request.data.codTipoConcepto", target = "codTipoConcepto")
    @Mapping(source = "request.data.codTipoMotivo", target = "codTipoMotivo")
    @Mapping(source = "request.data.valDescripcion", target = "valDescripcion")
    @Mapping(source = "request.data.codParentescoBeneficiario", target = "codParentescoBeneficiario")
    @Mapping(source = "request.data.flagGrupoEmpresarialBeneficiario", target = "flagGrupoEmpresarialBeneficiario")
    @Mapping(source = "request.data.valTelefonoBeneficiario", target = "valTelefonoBeneficiario")
    @Mapping(source = "request.data.valNombreBeneficiario", target = "valNombreBeneficiario")
    @Mapping(source = "request.data.valNumeroCuentaBeneficiario", target = "valNumeroCuentaBeneficiario")
    @Mapping(source = "request.data.codPaisOrigen", target = "codPaisOrigen")
    @Mapping(source = "request.data.codPaisDestino", target = "codPaisDestino")
    @Mapping(source = "request.data.flagReversa", target = "flagReversa")
    @Mapping(source = "request.data.valSecReverso", target = "valSecReverso")
    TransferCommand toCommand(RequestHeaders headers, OrqRequest request);

    /**
     * Convierte TransferResult (resultado de aplicación) a OrqResponse (DTO REST).
     * 
     * Mapea el resultado plano del negocio hacia la estructura jerárquica requerida por el contrato REST.
     */
    @Mapping(target = "nombredelServicioResponse", constant = "OrqCompensacion")
    @Mapping(target = "dataHeader.nombreOperacion", source = "nombreOperacion")
    @Mapping(target = "dataHeader.total", source = "total")
    @Mapping(target = "dataHeader.caracterAceptacion", source = "caracterAceptacion")
    @Mapping(target = "dataHeader.ultimoMensaje", source = "ultimoMensaje")
    @Mapping(target = "dataHeader.idTransaccion", source = "idTransaccion")
    @Mapping(target = "dataHeader.codMsgRespuesta", source = "codMsgRespuesta")
    @Mapping(target = "dataHeader.msgRespuesta", source = "msgRespuesta")
    @Mapping(target = "data.valNumeroComprobante", source = "valNumeroComprobante")
    @Mapping(target = "data.valSecuencial", source = "valSecuencial")
    @Mapping(target = "data.fecHoraMovimiento", source = "fecHoraMovimiento")
    @Mapping(target = "data.valMonto", source = "valMonto")
    @Mapping(target = "data.costoDeLaTransaccion", source = "costoDeLaTransaccion")
    @Mapping(target = "data.valTasaCambio", source = "valTasaCambio")
    @Mapping(target = "data.valMontoDestino", source = "valMontoDestino")
    @Mapping(target = "data.codMonedaTransaccion", source = "codMonedaTransaccion")
    OrqResponse toResponse(TransferResult result);

    /**
     * Método helper para inicializar DataHeaderResponse si es necesario.
     * MapStruct puede usar este método durante el mapeo.
     */
    default DataHeaderResponse createDataHeaderResponse() {
        return new DataHeaderResponse();
    }

    /**
     * Método helper para inicializar DataResponse si es necesario.
     * MapStruct puede usar este método durante el mapeo.
     */
    default DataResponse createDataResponse() {
        return new DataResponse();
    }
}
