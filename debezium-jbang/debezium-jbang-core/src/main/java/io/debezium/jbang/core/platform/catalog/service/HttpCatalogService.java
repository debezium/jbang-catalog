/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.service;

import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.core.auth.BearerTokenFilter;
import io.debezium.jbang.core.platform.catalog.api.CatalogAPI;
import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.vertx.core.http.HttpClientOptions;

public class HttpCatalogService implements CatalogService {

    private final CatalogAPI catalogAPI;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpCatalogService(URI platformAddress) {
        this.catalogAPI = QuarkusRestClientBuilder.newBuilder()
                .baseUri(platformAddress)
                .httpClientOptions(new HttpClientOptions())
                .register(new BearerTokenFilter(platformAddress.toString()))
                .build(CatalogAPI.class);
    }

    @Override
    public String getCatalog(String type) {
        return catalogAPI.getCatalog(type);
    }

    @Override
    public ComponentDescriptor getComponentDescriptor(String type, String componentClass) {
        try {
            String json = catalogAPI.getComponentDescriptor(type, componentClass);
            return objectMapper.readValue(json, ComponentDescriptor.class);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse component descriptor for " + componentClass, e);
        }
    }
}
