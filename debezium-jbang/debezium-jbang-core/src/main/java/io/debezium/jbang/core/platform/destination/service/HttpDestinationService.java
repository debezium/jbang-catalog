/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.destination.service;

import java.net.URI;
import java.util.List;

import io.debezium.jbang.core.auth.BearerTokenFilter;
import io.debezium.jbang.core.platform.destination.api.DestinationAPI;
import io.debezium.jbang.core.platform.destination.dto.DestinationRequest;
import io.debezium.jbang.core.platform.destination.dto.DestinationResponse;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.vertx.core.http.HttpClientOptions;

public class HttpDestinationService implements DestinationService {

    private final DestinationAPI destinationAPI;

    public HttpDestinationService(URI platformAddress) {
        this.destinationAPI = QuarkusRestClientBuilder.newBuilder()
                .baseUri(platformAddress)
                .httpClientOptions(new HttpClientOptions())
                .register(new BearerTokenFilter(platformAddress.toString()))
                .build(DestinationAPI.class);
    }

    @Override
    public List<DestinationResponse> listDestinations() {
        return destinationAPI.listDestinations();
    }

    @Override
    public DestinationResponse getDestination(Long id) {
        return destinationAPI.getDestination(id);
    }

    @Override
    public DestinationResponse createDestination(DestinationRequest request) {
        return destinationAPI.createDestination(request);
    }

    @Override
    public DestinationResponse updateDestination(Long id, DestinationRequest request) {
        return destinationAPI.updateDestination(id, request);
    }

    @Override
    public void deleteDestination(Long id) {
        destinationAPI.deleteDestination(id);
    }
}
