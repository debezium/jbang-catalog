/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog;

import java.util.List;
import java.util.Map;

public record Catalog(
        Schema schema,
        Build build,
        Map<String, List<Component>> components) {
    public record Schema(Integer version) {
    }

    public record Build(String version) {
    }

    public record Component(String componentClass, String name, String description, String descriptor) {
    }
}
