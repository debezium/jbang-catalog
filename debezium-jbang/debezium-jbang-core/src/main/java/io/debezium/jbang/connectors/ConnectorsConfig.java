/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.connectors;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConnectorsConfig(
        @JsonProperty("connectors") List<ConnectorRegistry.ConnectorEntry> connectors,
        @JsonProperty("sinks") List<ConnectorRegistry.SinkEntry> sinks,
        @JsonProperty("extras") List<ConnectorRegistry.ExtrasEntry> extras) {

    public ConnectorRegistry.ConnectorEntry getConnector(String name) {
        return this.connectors().stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown connector: " + name));
    }

    public ConnectorRegistry.SinkEntry getSink(String name) {
        return this.sinks().stream()
                .filter(s -> s.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown sink: " + name));
    }

    public ConnectorRegistry.ExtrasEntry getExtra(String name) {
        return this.extras().stream()
                .filter(e -> e.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown extra: " + name));
    }

    public Map<String, String> getConnectorsNameByClass() {
        return this.connectors().stream()
                .collect(Collectors.toMap(ConnectorRegistry.ConnectorEntry::name, ConnectorRegistry.ConnectorEntry::connectorClass));
    }
}
