/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.pipeline.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PipelineRequest(
        String name,
        String description,
        NamedRef source,
        NamedRef destination,
        List<NamedRef> transforms,
        String logLevel,
        Map<String, String> logLevels) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamedRef(Long id, String name) {
    }
}
