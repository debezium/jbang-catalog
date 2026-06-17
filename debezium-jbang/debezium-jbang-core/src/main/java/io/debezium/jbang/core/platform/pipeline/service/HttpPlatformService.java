/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.pipeline.service;

import java.net.URI;
import java.util.List;

import io.debezium.jbang.core.platform.pipeline.api.PipelineAPI;
import io.debezium.jbang.core.platform.pipeline.dto.PipelineRequest;
import io.debezium.jbang.core.platform.pipeline.dto.PipelineResponse;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.vertx.core.http.HttpClientOptions;

public class HttpPlatformService implements PlatformService {

    private final PipelineAPI pipelineAPI;

    public HttpPlatformService(URI platformAddress) {
        this.pipelineAPI = QuarkusRestClientBuilder.newBuilder()
                .baseUri(platformAddress)
                .httpClientOptions(new HttpClientOptions())
                .build(PipelineAPI.class);

    }

    @Override
    public List<PipelineResponse> listPipelines() {
        return pipelineAPI.listPipelines();
    }

    @Override
    public PipelineResponse getPipeline(Long id) {
        return pipelineAPI.getPipeline(id);
    }

    @Override
    public PipelineResponse createPipeline(PipelineRequest request) {
        return pipelineAPI.createPipeline(request);
    }

    @Override
    public PipelineResponse updatePipeline(Long id, PipelineRequest request) {
        return pipelineAPI.updatePipeline(id, request);
    }

    @Override
    public void deletePipeline(Long id) {
        pipelineAPI.deletePipeline(id);
    }
}
