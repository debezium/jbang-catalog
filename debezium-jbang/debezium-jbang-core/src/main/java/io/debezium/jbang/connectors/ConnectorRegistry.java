/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.connectors;

import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.dataformat.yaml.YAMLMapper;

public class ConnectorRegistry {

    private static ConnectorsConfig config = null;

    public static ConnectorsConfig getInstance(YAMLMapper mapper) {

        if (config != null) {
            return config;
        }

        try (InputStream is = ConnectorRegistry.class.getClassLoader().getResourceAsStream("connectors.yaml")) {
            if (is == null) {
                throw new RuntimeException("connectors.yaml not found on classpath");
            }

            return mapper.readValue(is, ConnectorsConfig.class);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load connectors.yaml", e);
        }
    }

    public record ConnectorEntry(
            @JsonProperty("name") String name,
            @JsonProperty("class") String connectorClass,
            @JsonProperty("artifact") String artifact,
            @JsonProperty("profile") String profile,
            @JsonProperty("versions") List<String> versions,
            @JsonProperty("sinks") List<String> sinks) {
    }

    public record SinkEntry(
            @JsonProperty("name") String name,
            @JsonProperty("artifact") String artifact,
            @JsonProperty("profile") String profile) {
    }

    public record ExtrasEntry(
            @JsonProperty("name") String name,
            @JsonProperty("profile") String profile) {
    }

}
