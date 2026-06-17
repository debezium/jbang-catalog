/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.pipeline.mapper;

import java.util.List;

import io.debezium.jbang.core.platform.pipeline.Pipeline;
import io.debezium.jbang.core.platform.pipeline.dto.PipelineResponse;

public class PipelineMapper {

    private PipelineMapper() {
    }

    public static Pipeline toDomain(PipelineResponse response) {
        return new Pipeline(
                response.id(),
                response.name(),
                response.description(),
                response.source() != null ? new Pipeline.NamedRef(response.source().id(), response.source().name()) : null,
                response.destination() != null ? new Pipeline.NamedRef(response.destination().id(), response.destination().name()) : null,
                response.transforms() != null ? response.transforms().stream()
                        .map(t -> new Pipeline.NamedRef(t.id(), t.name()))
                        .toList() : null,
                response.logLevel(),
                response.logLevels());
    }

    public static List<Pipeline> toDomain(List<PipelineResponse> responses) {
        return responses.stream().map(PipelineMapper::toDomain).toList();
    }
}
