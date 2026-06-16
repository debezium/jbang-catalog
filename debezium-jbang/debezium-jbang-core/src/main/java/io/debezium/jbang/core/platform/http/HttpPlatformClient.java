/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.http;

import java.net.URI;
import java.util.List;

import io.debezium.jbang.core.platform.PlatformClient;
import io.debezium.jbang.core.platform.dto.PipelineRequest;
import io.debezium.jbang.core.platform.dto.PipelineResponse;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.vertx.core.http.HttpClientOptions;

public class HttpPlatformClient implements PlatformClient {

    private final PipelineRestClient client;
    private final String baseUrl;

    public HttpPlatformClient(String apiUrl) {
        this.client = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(apiUrl))
                .httpClientOptions(new HttpClientOptions())
                .build(PipelineRestClient.class);

        this.baseUrl = apiUrl;
    }

    @Override
    public List<PipelineResponse> listPipelines() {
        return client.listPipelines();
    }

    @Override
    public PipelineResponse getPipeline(Long id) {
        return client.getPipeline(id);
    }

    @Override
    public PipelineResponse createPipeline(PipelineRequest request) {
        return client.createPipeline(request);
    }

    @Override
    public PipelineResponse updatePipeline(Long id, PipelineRequest request) {
        return client.updatePipeline(id, request);
    }

    @Override
    public void deletePipeline(Long id) {
        client.deletePipeline(id);
    }
}
