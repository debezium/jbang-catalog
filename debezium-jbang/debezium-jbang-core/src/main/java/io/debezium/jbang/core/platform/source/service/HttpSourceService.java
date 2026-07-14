/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.source.service;

import java.net.URI;
import java.util.List;

import io.debezium.jbang.core.platform.source.api.SourceAPI;
import io.debezium.jbang.core.platform.source.dto.SignalCollectionVerifyRequest;
import io.debezium.jbang.core.platform.source.dto.SignalVerificationResult;
import io.debezium.jbang.core.platform.source.dto.SourceRequest;
import io.debezium.jbang.core.platform.source.dto.SourceResponse;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.vertx.core.http.HttpClientOptions;

public class HttpSourceService implements SourceService {

    private final SourceAPI sourceAPI;

    public HttpSourceService(URI platformAddress) {
        this.sourceAPI = QuarkusRestClientBuilder.newBuilder()
                .baseUri(platformAddress)
                .httpClientOptions(new HttpClientOptions())
                .build(SourceAPI.class);
    }

    @Override
    public List<SourceResponse> listSources() {
        return sourceAPI.listSources();
    }

    @Override
    public SourceResponse getSource(Long id) {
        return sourceAPI.getSource(id);
    }

    @Override
    public SourceResponse createSource(SourceRequest request) {
        return sourceAPI.createSource(request);
    }

    @Override
    public SourceResponse updateSource(Long id, SourceRequest request) {
        return sourceAPI.updateSource(id, request);
    }

    @Override
    public void deleteSource(Long id) {
        sourceAPI.deleteSource(id);
    }

    @Override
    public SignalVerificationResult verifySignals(SignalCollectionVerifyRequest request) {
        return sourceAPI.verifySignals(request);
    }
}
