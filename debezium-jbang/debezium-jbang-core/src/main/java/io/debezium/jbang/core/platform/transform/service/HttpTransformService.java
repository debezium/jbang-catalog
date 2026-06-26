/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.transform.service;

import java.net.URI;
import java.util.List;

import io.debezium.jbang.core.platform.transform.api.TransformAPI;
import io.debezium.jbang.core.platform.transform.dto.TransformRequest;
import io.debezium.jbang.core.platform.transform.dto.TransformResponse;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.vertx.core.http.HttpClientOptions;

public class HttpTransformService implements TransformService {

    private final TransformAPI transformAPI;

    public HttpTransformService(URI platformAddress) {
        this.transformAPI = QuarkusRestClientBuilder.newBuilder()
                .baseUri(platformAddress)
                .httpClientOptions(new HttpClientOptions())
                .build(TransformAPI.class);
    }

    @Override
    public List<TransformResponse> listTransforms() {
        return transformAPI.listTransforms();
    }

    @Override
    public TransformResponse getTransform(Long id) {
        return transformAPI.getTransform(id);
    }

    @Override
    public TransformResponse createTransform(TransformRequest request) {
        return transformAPI.createTransform(request);
    }

    @Override
    public TransformResponse updateTransform(Long id, TransformRequest request) {
        return transformAPI.updateTransform(id, request);
    }

    @Override
    public void deleteTransform(Long id) {
        transformAPI.deleteTransform(id);
    }
}
