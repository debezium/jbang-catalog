/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.debezium.jbang.core.platform.dto.PipelineResponse;

class PipelineMapperTest {

    @Test
    void shouldMapFullResponse() {
        PipelineResponse response = new PipelineResponse(
                1L, "my-pipeline", "A pipeline",
                new PipelineResponse.NamedRef(10L, "src"),
                new PipelineResponse.NamedRef(20L, "dst"),
                List.of(new PipelineResponse.NamedRef(30L, "tx")),
                "INFO",
                Map.of("io.debezium", "DEBUG"));

        Pipeline p = PipelineMapper.toDomain(response);

        assertThat(p.id()).isEqualTo(1L);
        assertThat(p.name()).isEqualTo("my-pipeline");
        assertThat(p.description()).isEqualTo("A pipeline");
        assertThat(p.source()).isEqualTo(new Pipeline.NamedRef(10L, "src"));
        assertThat(p.destination()).isEqualTo(new Pipeline.NamedRef(20L, "dst"));
        assertThat(p.transforms()).containsExactly(new Pipeline.NamedRef(30L, "tx"));
        assertThat(p.logLevel()).isEqualTo("INFO");
        assertThat(p.logLevels()).containsEntry("io.debezium", "DEBUG");
    }

    @Test
    void shouldHandleNullOptionalFields() {
        PipelineResponse response = new PipelineResponse(
                2L, "minimal", null, null, null, null, null, null);

        Pipeline p = PipelineMapper.toDomain(response);

        assertThat(p.id()).isEqualTo(2L);
        assertThat(p.name()).isEqualTo("minimal");
        assertThat(p.description()).isNull();
        assertThat(p.source()).isNull();
        assertThat(p.destination()).isNull();
        assertThat(p.transforms()).isNull();
        assertThat(p.logLevel()).isNull();
        assertThat(p.logLevels()).isNull();
    }

    @Test
    void shouldMapList() {
        List<PipelineResponse> responses = List.of(
                new PipelineResponse(1L, "a", null, null, null, null, null, null),
                new PipelineResponse(2L, "b", null, null, null, null, null, null));

        List<Pipeline> result = PipelineMapper.toDomain(responses);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("a");
        assertThat(result.get(1).name()).isEqualTo("b");
    }
}
