/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.debezium.jbang.core.platform.transform.Transform;
import io.debezium.jbang.core.platform.transform.dto.PredicateDto;
import io.debezium.jbang.core.platform.transform.dto.TransformResponse;
import io.debezium.jbang.core.platform.transform.mapper.TransformMapper;

class TransformMapperTest {

    @Test
    void shouldMapFullResponse() {
        TransformResponse response = new TransformResponse(
                1L, "my-transform", "A transform", "filter", "io.debezium.filter",
                Set.of(new TransformResponse.NamedRef(10L, "vault")),
                Map.of("key", "value"),
                new PredicateDto("topicNameMatches", Map.of("pattern", "orders"), true));

        Transform t = TransformMapper.toDomain(response);

        assertThat(t.id()).isEqualTo(1L);
        assertThat(t.name()).isEqualTo("my-transform");
        assertThat(t.description()).isEqualTo("A transform");
        assertThat(t.type()).isEqualTo("filter");
        assertThat(t.schema()).isEqualTo("io.debezium.filter");
        assertThat(t.vaults()).containsExactly(new Transform.NamedRef(10L, "vault"));
        assertThat(t.config()).containsEntry("key", "value");
        assertThat(t.predicate()).isEqualTo(new Transform.Predicate("topicNameMatches", Map.of("pattern", "orders"), true));
    }

    @Test
    void shouldHandleNullOptionalFields() {
        TransformResponse response = new TransformResponse(
                2L, "minimal", null, "filter", "io.debezium.filter", null, null, null);

        Transform t = TransformMapper.toDomain(response);

        assertThat(t.id()).isEqualTo(2L);
        assertThat(t.name()).isEqualTo("minimal");
        assertThat(t.description()).isNull();
        assertThat(t.vaults()).isNull();
        assertThat(t.config()).isNull();
        assertThat(t.predicate()).isNull();
    }

    @Test
    void shouldMapList() {
        List<TransformResponse> responses = List.of(
                new TransformResponse(1L, "a", null, "filter", "io.debezium.filter", null, null, null),
                new TransformResponse(2L, "b", null, "filter", "io.debezium.filter", null, null, null));

        List<Transform> result = TransformMapper.toDomain(responses);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("a");
        assertThat(result.get(1).name()).isEqualTo("b");
    }
}
