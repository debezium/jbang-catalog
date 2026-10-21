/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core;

import io.quarkus.runtime.annotations.RegisterForReflection;

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
})
public class ConfigurationReflectionRegistration {
}