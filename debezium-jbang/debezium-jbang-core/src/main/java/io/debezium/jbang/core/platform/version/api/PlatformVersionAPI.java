/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.version.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.debezium.jbang.core.platform.version.dto.PlatformVersionInfo;

@Path("/q")
public interface PlatformVersionAPI {

    @GET
    @Path("/openapi")
    @Produces(MediaType.APPLICATION_JSON)
    PlatformVersionInfo getInfo();
}
