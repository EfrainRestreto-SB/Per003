package org.acme.webapi.controllers;

import java.util.Optional;

import org.acme.domain.interfaces.usecases.Per003UseCase;
import org.acme.domain.models.requests.Per003RequestModel;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("per003")
public class Per003Controller {

    @Inject
    private Per003UseCase Per003UseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessage(Per003RequestModel Per003RequestModel, @Context HttpHeaders httpHeaders) {

        Optional<String> header1 = getSpecificHeader(httpHeaders, "header1");
        if(header1.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Header header1 no enviado").build();
        }

        Optional<String> header2 = getSpecificHeader(httpHeaders, "header2");
        if(header2.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Header header2 no enviado").build();
        }

        return Response.ok(Per003UseCase.getMessage(Per003RequestModel)).build();
    }

    private Optional<String> getSpecificHeader(HttpHeaders httpHeaders, String headerName) {
        String header = httpHeaders.getHeaderString(headerName);
        return Optional.ofNullable(header);
    }
}
