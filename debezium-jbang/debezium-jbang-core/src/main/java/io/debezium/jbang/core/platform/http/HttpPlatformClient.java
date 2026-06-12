/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.http;

import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import io.debezium.jbang.core.platform.PlatformClient;
import io.debezium.jbang.core.platform.dto.PipelineRequest;
import io.debezium.jbang.core.platform.dto.PipelineResponse;

public class HttpPlatformClient implements PlatformClient {

    private final Client client;
    private final String baseUrl;

    public HttpPlatformClient(String apiUrl) {
        this.client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        this.baseUrl = apiUrl;
    }

    @Override
    public List<PipelineResponse> listPipelines() {
        return client.target(baseUrl).path("/api/pipelines")
                .request(MediaType.APPLICATION_JSON)
                .get(new GenericType<List<PipelineResponse>>() {
                });
    }

    @Override
    public PipelineResponse getPipeline(Long id) {
        return client.target(baseUrl).path("/api/pipelines").path(String.valueOf(id))
                .request(MediaType.APPLICATION_JSON)
                .get(PipelineResponse.class);
    }

    @Override
    public PipelineResponse createPipeline(PipelineRequest request) {
        return client.target(baseUrl).path("/api/pipelines")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(request), PipelineResponse.class);
    }

    @Override
    public PipelineResponse updatePipeline(Long id, PipelineRequest request) {
        return client.target(baseUrl).path("/api/pipelines").path(String.valueOf(id))
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(request), PipelineResponse.class);
    }

    @Override
    public void deletePipeline(Long id) {
        client.target(baseUrl).path("/api/pipelines").path(String.valueOf(id))
                .request()
                .delete();
    }
}
