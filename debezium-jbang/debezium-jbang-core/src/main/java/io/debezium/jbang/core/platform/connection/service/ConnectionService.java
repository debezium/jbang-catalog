/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.connection.service;

import java.util.List;

import io.debezium.jbang.core.platform.connection.dto.CollectionTree;
import io.debezium.jbang.core.platform.connection.dto.ConnectionRequest;
import io.debezium.jbang.core.platform.connection.dto.ConnectionResponse;
import io.debezium.jbang.core.platform.connection.dto.ConnectionValidationResult;

public interface ConnectionService {

    List<ConnectionResponse> listConnections();

    ConnectionResponse getConnection(Long id);

    ConnectionResponse createConnection(ConnectionRequest connectionRequest);

    ConnectionResponse updateConnection(Long id, ConnectionRequest connectionRequest);

    void deleteConnection(Long id);

    ConnectionValidationResult validateConnection(ConnectionRequest connectionRequest);

    String connectionSchemas();

    CollectionTree listAvailableCollections(Long id);
}
