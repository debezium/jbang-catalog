/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.http;

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

import io.debezium.jbang.core.platform.dto.PipelineRequest;
import io.debezium.jbang.core.platform.dto.PipelineResponse;

@Path("/api/pipelines")
public interface PipelineRestClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<PipelineResponse> listPipelines();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    PipelineResponse getPipeline(@PathParam("id") Long id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    PipelineResponse createPipeline(PipelineRequest request);

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    PipelineResponse updatePipeline(@PathParam("id") Long id, PipelineRequest request);

    @DELETE
    @Path("/{id}")
    void deletePipeline(@PathParam("id") Long id);
}
