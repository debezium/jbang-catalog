/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.transform.api;

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

import io.debezium.jbang.core.platform.transform.dto.TransformRequest;
import io.debezium.jbang.core.platform.transform.dto.TransformResponse;

@Path("/api/transforms")
public interface TransformAPI {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<TransformResponse> listTransforms();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    TransformResponse getTransform(@PathParam("id") Long id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    TransformResponse createTransform(TransformRequest request);

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    TransformResponse updateTransform(@PathParam("id") Long id, TransformRequest request);

    @DELETE
    @Path("/{id}")
    void deleteTransform(@PathParam("id") Long id);
}
