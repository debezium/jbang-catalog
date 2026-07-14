/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.source.api;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.debezium.jbang.core.platform.source.dto.SignalCollectionVerifyRequest;
import io.debezium.jbang.core.platform.source.dto.SignalVerificationResult;
import io.debezium.jbang.core.platform.source.dto.SourceRequest;
import io.debezium.jbang.core.platform.source.dto.SourceResponse;

@Path("/api/sources")
public interface SourceAPI {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<SourceResponse> listSources();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    SourceResponse getSource(@PathParam("id") Long id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    SourceResponse createSource(SourceRequest request);

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    SourceResponse updateSource(@PathParam("id") Long id, SourceRequest request);

    @DELETE
    @Path("/{id}")
    void deleteSource(@PathParam("id") Long id);

    @POST
    @Path("/signals/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    SignalVerificationResult verifySignals(SignalCollectionVerifyRequest request);
}
