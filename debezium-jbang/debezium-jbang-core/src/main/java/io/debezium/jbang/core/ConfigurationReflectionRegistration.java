/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * HybridJacksonPool is only present in the Java 21 section of the multi-release vertx-core jar,
 * so it can't be referenced as a class literal when compiling with --release 17: use classNames.
 * In any case HybridJacksonPool shouldn't be registered for reflection here. It's done in the
 * quarkus extension processor at build time but for some reason it isn't executed
 */
@RegisterForReflection(targets = {
        io.debezium.jbang.configuration.Configuration.class,
        io.debezium.jbang.configuration.Platform.class,
        io.debezium.jbang.configuration.Environment.class,
        io.debezium.jbang.configuration.Build.class,
        io.debezium.jbang.configuration.Container.class,
        io.debezium.jbang.configuration.Base.class,
        io.debezium.jbang.configuration.Maven.class,
        io.debezium.jbang.configuration.Oci.class,
        io.debezium.jbang.configuration.Registry.class,
        io.debezium.jbang.configuration.Artifact.class,
        io.debezium.jbang.connectors.ConnectorsConfig.class,
        io.debezium.jbang.connectors.ConnectorRegistry.ConnectorEntry.class,
        io.debezium.jbang.connectors.ConnectorRegistry.SinkEntry.class,
        io.debezium.jbang.connectors.ConnectorRegistry.ExtrasEntry.class,
        io.debezium.jbang.connectors.ConnectorManifest.class,
}, classNames = {
        "io.vertx.core.json.jackson.v3.HybridJacksonPool",
})
public class ConfigurationReflectionRegistration {
}