/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.pipeline;

import java.util.List;
import java.util.Map;

public record Pipeline(
        Long id,
        String name,
        String description,
        NamedRef source,
        NamedRef destination,
        List<NamedRef> transforms,
        String logLevel,
        Map<String, String> logLevels) {

    public record NamedRef(Long id, String name) {
    }
}
