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

import io.debezium.jbang.core.platform.connection.Connection;
import io.debezium.jbang.core.platform.connection.dto.ConnectionResponse;
import io.debezium.jbang.core.platform.connection.mapper.ConnectionMapper;

class ConnectionMapperTest {

    @Test
    void shouldMapFullResponse() {
        ConnectionResponse response = new ConnectionResponse(
                1L, "my-connection", "POSTGRESQL", Map.of("key", "value"));

        Connection c = ConnectionMapper.toDomain(response);

        assertThat(c.id()).isEqualTo(1L);
        assertThat(c.name()).isEqualTo("my-connection");
        assertThat(c.type()).isEqualTo("POSTGRESQL");
        assertThat(c.config()).containsEntry("key", "value");
    }

    @Test
    void shouldHandleNullOptionalFields() {
        ConnectionResponse response = new ConnectionResponse(2L, "minimal", "KAFKA", null);

        Connection c = ConnectionMapper.toDomain(response);

        assertThat(c.id()).isEqualTo(2L);
        assertThat(c.name()).isEqualTo("minimal");
        assertThat(c.config()).isNull();
    }

    @Test
    void shouldMapList() {
        List<ConnectionResponse> responses = List.of(
                new ConnectionResponse(1L, "a", "POSTGRESQL", null),
                new ConnectionResponse(2L, "b", "KAFKA", null));

        List<Connection> result = ConnectionMapper.toDomain(responses);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("a");
        assertThat(result.get(1).name()).isEqualTo("b");
    }
}
