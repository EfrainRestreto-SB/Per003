package pa.davivienda.webapi.controllers;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;
import pa.davivienda.domain.dtos.requests.OrqRequest;
import pa.davivienda.domain.dtos.responses.ErrorResponse;
import pa.davivienda.domain.dtos.responses.OrqResponse;
import pa.davivienda.domain.interfaces.usecases.OrqCompensacionService;
import pa.davivienda.webapi.dto.RequestHeaders;
import pa.davivienda.webapi.mappers.TransferMapper;

/**
 * Controlador REST para orquestación de compensaciones.
 * 
 * Arquitectura de capas (Hexagonal/Clean Architecture):
 * 
 * 1. CAPA WEB API (este controlador):
 *    - Recibe OrqRequest (DTO REST) del cliente
 *    - Valida el payload usando Bean Validation (@Valid)
 *    - Usa TransferMapper (MapStruct) para convertir OrqRequest → TransferCommand
 *    - Invoca el servicio de aplicación pasando TransferCommand
 *    - Recibe TransferResult del servicio
 *    - Usa TransferMapper para convertir TransferResult → OrqResponse
 *    - Devuelve OrqResponse (DTO REST) al cliente
 * 
 * 2. MAPPER (TransferMapper):
 *    - Actúa como adaptador entre la capa web y la capa de aplicación
 *    - Convierte DTOs REST ↔ Commands/Results del dominio
 *    - Desacopla el contrato REST de la lógica de negocio
 * 
 * 3. CAPA DE APLICACIÓN (OrqCompensacionService):
 *    - Contiene la lógica de negocio pura
 *    - Recibe TransferCommand (independiente del protocolo)
 *    - Devuelve TransferResult (independiente del protocolo)
 *    - No conoce nada sobre HTTP, JSON, validaciones REST, etc.
 * 
 * Beneficios de esta arquitectura:
 * - La lógica de negocio es testeable sin dependencias de frameworks web
 * - Podemos cambiar el protocolo (REST → gRPC, GraphQL) sin tocar la lógica
 * - Los Commands/Results son objetos puros del dominio, sin anotaciones de frameworks
 * - Cumple con el principio de inversión de dependencias (DIP)
 */
@Path("/orqcompensacion/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Orquestación de Compensaciones", description = "API para gestión de transferencias y compensaciones")
public class OrqCompensacionResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrqCompensacionResource.class);

    @Inject
    OrqCompensacionService service;

    @Inject
    TransferMapper mapper;

    /**
     * Endpoint principal para orquestación de compensaciones.
     *
     * Flujo del request:
     * 1. Headers HTTP (RequestHeaders) + Body JSON (OrqRequest) → validación con @Valid
     * 2. Headers + OrqRequest → TransferCommand (mapper)
     * 3. TransferCommand → service.transfer() → TransferResult
     * 4. TransferResult → OrqResponse (mapper)
     * 5. OrqResponse → cliente
     * 
     * El mapper (MapStruct) se encarga de la conversión automática entre:
     * - Headers HTTP (RequestHeaders): metadata del request (nombreOperacion, total, jornada, etc.)
     * - Body JSON (OrqRequest.Data): datos de negocio de la transferencia
     * - Comandos/Resultados (TransferCommand/TransferResult): estructura plana
     * 
     * @param headers Headers HTTP con DataHeader (nombreOperacion, total, jornada, canal, usuario, etc.)
     * @param request Body JSON con los datos de la transferencia (Data)
     * @return Response con OrqResponse o ErrorResponse
     */
    @POST
    @Path("/transfer")
    @Operation(
        summary = "Ejecutar transferencia/compensación",
        description = "Orquesta una transferencia o compensación entre cuentas. " +
                      "DataHeader viene en HTTP headers. Data viene en el body JSON. " +
                      "Soporta múltiples tipos de operaciones: COBPER (Cobro Membresía), " +
                      "TRCPRO (Transferencia Regional Cuentas Propias), " +
                      "TRCTER (Transferencia Regional a Terceros), " +
                      "TININD (Transferencia Internacional Individual)."
    )
    @Parameters({
        @Parameter(name = "nombreOperacion", in = ParameterIn.HEADER, required = true, description = "Nombre de la operación"),
        @Parameter(name = "total", in = ParameterIn.HEADER, required = true, description = "Total de transacciones"),
        @Parameter(name = "jornada", in = ParameterIn.HEADER, required = true, description = "Código de jornada"),
        @Parameter(name = "canal", in = ParameterIn.HEADER, required = true, description = "Código de canal"),
        @Parameter(name = "modoDeOperacion", in = ParameterIn.HEADER, required = true, description = "Modo de operación"),
        @Parameter(name = "usuario", in = ParameterIn.HEADER, required = true, description = "Usuario que ejecuta la operación"),
        @Parameter(name = "perfil", in = ParameterIn.HEADER, required = true, description = "Perfil del usuario"),
        @Parameter(name = "versionServicio", in = ParameterIn.HEADER, required = true, description = "Versión del servicio"),
        @Parameter(name = "idTransaccion", in = ParameterIn.HEADER, required = true, description = "ID de transacción para trazabilidad")
    })
    @RequestBody(
        description = "Datos de la transferencia/compensación",
        required = true,
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = OrqRequest.class)
        )
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Transferencia ejecutada exitosamente",
            content = @Content(schema = @Schema(implementation = OrqResponse.class))
        ),
        @APIResponse(
            responseCode = "400",
            description = "Petición inválida - errores de validación o parámetros incorrectos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @APIResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public Response transfer(
            @Valid RequestHeaders headers,
            @Valid OrqRequest request) {
        
        String correlationId = null;
        try {
            // 1) Asegurar idTransaccion: si viene null, generar uno.
            if (headers.getIdTransaccion() == null || headers.getIdTransaccion().isBlank()) {
                correlationId = UUID.randomUUID().toString();
                headers.setIdTransaccion(correlationId);
            } else {
                correlationId = headers.getIdTransaccion();
            }

            // 2) Propagar correlationId en MDC (logs)
            MDC.put("idTransaccion", correlationId);
            LOG.info("Inicio OrqCompensacion - transfer - idTransaccion={}, usuario={}, operacion={}", 
                     correlationId, headers.getUsuario(), headers.getNombreOperacion());
            
            // 3) Convertir Headers HTTP + Body JSON → TransferCommand (comando de aplicación)
            //    Este mapper desacopla la estructura REST de la lógica de negocio
            TransferCommand command = mapper.toCommand(headers, request);
            LOG.debug("Request mapeado a comando - concepto={}, monto={}", 
                     command.getCodTipoConcepto(), command.getValMonto());
            
            // 4) Llamada al servicio de aplicación (caso de uso) - abstracción hexagonal
            //    El servicio no conoce nada sobre HTTP, JSON, headers o estructura REST
            TransferResult result = service.transfer(command);
            LOG.debug("Resultado obtenido del servicio - comprobante={}", 
                     result.getValNumeroComprobante());
            
            // 5) Convertir TransferResult (resultado de aplicación) → OrqResponse (DTO REST)
            //    Este mapper reconstruye la estructura jerárquica esperada por el cliente REST
            OrqResponse response = mapper.toResponse(result);

            LOG.info("Fin OrqCompensacion - transfer - idTransaccion={}", correlationId);

            return Response.ok(response).build();
        } catch (jakarta.validation.ValidationException ve) {
            LOG.warn("Validación inválida - idTransaccion={}", correlationId, ve);
            ErrorResponse err = ErrorResponse.fromValidation(ve, correlationId);
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        } catch (IllegalArgumentException iae) {
            LOG.warn("Petición incorrecta - idTransaccion={}", correlationId, iae);
            ErrorResponse err = ErrorResponse.fromMessage(400, iae.getMessage(), correlationId);
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        } catch (Exception ex) {
            LOG.error("Error interno - idTransaccion={}", correlationId, ex);
            ErrorResponse err = ErrorResponse.fromMessage(500, "Error interno del servicio", correlationId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(err).build();
        } finally {
            MDC.remove("idTransaccion");
        }
    }
}

