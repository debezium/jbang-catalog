/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.transform;

import java.util.Map;
import java.util.Set;

public record Transform(
        Long id,
        String name,
        String description,
        String type,
        String schema,
        Set<NamedRef> vaults,
        Map<String, Object> config,
        Predicate predicate) {

    public record NamedRef(Long id, String name) {
    }

    public record Predicate(String type, Map<String, Object> config, boolean negate) {
    }
}
