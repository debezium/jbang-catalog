/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.build.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record DbzConfig(
        String version,
        String name,
        SourceConfig source,
        SinkConfig sink,
        BuildConfig build) {

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SourceConfig(String type, Map<String, Object> config) {
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SinkConfig(String type, Map<String, Object> config) {
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BuildConfig(ImageConfig image) {
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageConfig(String name, String tag, String registry) {
    }
}
