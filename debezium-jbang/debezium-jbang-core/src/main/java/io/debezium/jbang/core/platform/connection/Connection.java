/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.connection;

import java.util.Map;

public record Connection(
        Long id,
        String name,
        String type,
        Map<String, Object> config) {
}
