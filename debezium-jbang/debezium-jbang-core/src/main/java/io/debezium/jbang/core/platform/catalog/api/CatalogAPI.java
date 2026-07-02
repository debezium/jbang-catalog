/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/catalog")
public interface CatalogAPI {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    String getCatalog(@QueryParam("type") String type);

    @GET
    @Path("/{type}/{class}")
    @Produces(MediaType.APPLICATION_JSON)
    String getComponentDescriptor(@PathParam("type") String type, @PathParam("class") String componentClass);
}
