/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.connection.api;

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

import io.debezium.jbang.core.platform.connection.dto.CollectionTree;
import io.debezium.jbang.core.platform.connection.dto.ConnectionRequest;
import io.debezium.jbang.core.platform.connection.dto.ConnectionResponse;
import io.debezium.jbang.core.platform.connection.dto.ConnectionValidationResult;

@Path("/api/connections")
public interface ConnectionAPI {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ConnectionResponse> listConnections();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    ConnectionResponse getConnection(@PathParam("id") Long id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ConnectionResponse createConnection(ConnectionRequest request);

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ConnectionResponse updateConnection(@PathParam("id") Long id, ConnectionRequest request);

    @DELETE
    @Path("/{id}")
    void deleteConnection(@PathParam("id") Long id);

    @POST
    @Path("/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ConnectionValidationResult validateConnection(ConnectionRequest request);

    @GET
    @Path("/schemas")
    @Produces(MediaType.APPLICATION_JSON)
    String connectionSchemas();

    @GET
    @Path("/{id}/collections")
    @Produces(MediaType.APPLICATION_JSON)
    CollectionTree listAvailableCollections(@PathParam("id") Long id);
}
