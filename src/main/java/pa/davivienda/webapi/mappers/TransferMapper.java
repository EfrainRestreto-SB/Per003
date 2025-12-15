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

/**
 * Mapper MapStruct para convertir entre DTOs REST y Commands/Results del application layer.
 * 
 * Separación de responsabilidades (Hexagonal Architecture):
 * - OrqRequest/OrqResponse: DTOs de la capa de API (webapi) - acoplados al contrato REST
 * - TransferCommand/TransferResult: Objetos de la capa de aplicación - independientes del protocolo
 * 
 * Este mapper actúa como un adaptador entre la capa de presentación (REST) y la capa de aplicación.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface TransferMapper {

    /**
     * Convierte OrqRequest (DTO REST) a TransferCommand (comando de aplicación).
     * 
     * Mapea los campos desde DataHeader y Data hacia un objeto plano TransferCommand
     * para que la lógica de negocio no dependa de la estructura REST.
     */
    @Mapping(source = "dataHeader.nombreOperacion", target = "nombreOperacion")
    @Mapping(source = "dataHeader.total", target = "total")
    @Mapping(source = "dataHeader.jornada", target = "jornada")
    @Mapping(source = "dataHeader.canal", target = "canal")
    @Mapping(source = "dataHeader.modoDeOperacion", target = "modoDeOperacion")
    @Mapping(source = "dataHeader.usuario", target = "usuario")
    @Mapping(source = "dataHeader.perfil", target = "perfil")
    @Mapping(source = "dataHeader.versionServicio", target = "versionServicio")
    @Mapping(source = "dataHeader.idTransaccion", target = "idTransaccion")
    @Mapping(source = "data.idSesion", target = "idSesion")
    @Mapping(source = "data.codIdioma", target = "codIdioma")
    @Mapping(source = "data.valOrigen", target = "valOrigen")
    @Mapping(source = "data.codPais", target = "codPais")
    @Mapping(source = "data.valVersionApp", target = "valVersionApp")
    @Mapping(source = "data.codTipoIdentificacion", target = "codTipoIdentificacion")
    @Mapping(source = "data.valNumeroIdentificacion", target = "valNumeroIdentificacion")
    @Mapping(source = "data.codTipoProducto", target = "codTipoProducto")
    @Mapping(source = "data.valNumeroProducto", target = "valNumeroProducto")
    @Mapping(source = "data.codMonedaProducto", target = "codMonedaProducto")
    @Mapping(source = "data.codTipoMovimiento", target = "codTipoMovimiento")
    @Mapping(source = "data.valNumeroContrato", target = "valNumeroContrato")
    @Mapping(source = "data.valMonto", target = "valMonto")
    @Mapping(source = "data.valTasaCambio", target = "valTasaCambio")
    @Mapping(source = "data.valMontoDestino", target = "valMontoDestino")
    @Mapping(source = "data.codMonedaDestino", target = "codMonedaDestino")
    @Mapping(source = "data.codTipoConcepto", target = "codTipoConcepto")
    @Mapping(source = "data.codTipoMotivo", target = "codTipoMotivo")
    @Mapping(source = "data.valDescripcion", target = "valDescripcion")
    @Mapping(source = "data.codParentescoBeneficiario", target = "codParentescoBeneficiario")
    @Mapping(source = "data.flagGrupoEmpresarialBeneficiario", target = "flagGrupoEmpresarialBeneficiario")
    @Mapping(source = "data.valTelefonoBeneficiario", target = "valTelefonoBeneficiario")
    @Mapping(source = "data.valNombreBeneficiario", target = "valNombreBeneficiario")
    @Mapping(source = "data.valNumeroCuentaBeneficiario", target = "valNumeroCuentaBeneficiario")
    @Mapping(source = "data.codPaisOrigen", target = "codPaisOrigen")
    @Mapping(source = "data.codPaisDestino", target = "codPaisDestino")
    @Mapping(source = "data.flagReversa", target = "flagReversa")
    @Mapping(source = "data.valSecReverso", target = "valSecReverso")
    TransferCommand toCommand(OrqRequest request);

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
