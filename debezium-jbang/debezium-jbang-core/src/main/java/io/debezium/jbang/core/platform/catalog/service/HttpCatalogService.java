/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.service;

import java.net.URI;

import io.debezium.jbang.core.platform.catalog.api.CatalogAPI;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.vertx.core.http.HttpClientOptions;

public class HttpCatalogService implements CatalogService {

    private final CatalogAPI catalogAPI;

    public HttpCatalogService(URI platformAddress) {
        this.catalogAPI = QuarkusRestClientBuilder.newBuilder()
                .baseUri(platformAddress)
                .httpClientOptions(new HttpClientOptions())
                .build(CatalogAPI.class);
    }

    @Override
    public String getCatalog(String type) {
        return catalogAPI.getCatalog(type);
    }

    @Override
    public String getComponentDescriptor(String type, String componentClass) {
        return catalogAPI.getComponentDescriptor(type, componentClass);
    }
}
