/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.connection.service;

import java.net.URI;
import java.util.List;

import io.debezium.jbang.core.platform.connection.api.ConnectionAPI;
import io.debezium.jbang.core.platform.connection.dto.CollectionTree;
import io.debezium.jbang.core.platform.connection.dto.ConnectionRequest;
import io.debezium.jbang.core.platform.connection.dto.ConnectionResponse;
import io.debezium.jbang.core.platform.connection.dto.ConnectionValidationResult;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.vertx.core.http.HttpClientOptions;

public class HttpConnectionService implements ConnectionService {

    private final ConnectionAPI connectionAPI;

    public HttpConnectionService(URI platformAddress) {
        this.connectionAPI = QuarkusRestClientBuilder.newBuilder()
                .baseUri(platformAddress)
                .httpClientOptions(new HttpClientOptions())
                .build(ConnectionAPI.class);
    }

    @Override
    public List<ConnectionResponse> listConnections() {
        return connectionAPI.listConnections();
    }

    @Override
    public ConnectionResponse getConnection(Long id) {
        return connectionAPI.getConnection(id);
    }

    @Override
    public ConnectionResponse createConnection(ConnectionRequest request) {
        return connectionAPI.createConnection(request);
    }

    @Override
    public ConnectionResponse updateConnection(Long id, ConnectionRequest request) {
        return connectionAPI.updateConnection(id, request);
    }

    @Override
    public void deleteConnection(Long id) {
        connectionAPI.deleteConnection(id);
    }

    @Override
    public ConnectionValidationResult validateConnection(ConnectionRequest request) {
        return connectionAPI.validateConnection(request);
    }

    @Override
    public String connectionSchemas() {
        return connectionAPI.connectionSchemas();
    }

    @Override
    public CollectionTree listAvailableCollections(Long id) {
        return connectionAPI.listAvailableCollections(id);
    }
}
