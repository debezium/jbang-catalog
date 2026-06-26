/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.connection.mapper;

import java.util.List;

import io.debezium.jbang.core.platform.connection.Connection;
import io.debezium.jbang.core.platform.connection.dto.ConnectionResponse;

public class ConnectionMapper {

    private ConnectionMapper() {
    }

    public static Connection toDomain(ConnectionResponse response) {
        return new Connection(
                response.id(),
                response.name(),
                response.type(),
                response.config());
    }

    public static List<Connection> toDomain(List<ConnectionResponse> responses) {
        return responses.stream().map(ConnectionMapper::toDomain).toList();
    }
}
