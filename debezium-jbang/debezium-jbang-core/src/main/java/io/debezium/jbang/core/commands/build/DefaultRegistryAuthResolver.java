/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import java.util.Optional;

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.Registry;

public class DefaultRegistryAuthResolver implements RegistryAuthResolver {

    private static final String ENV_USERNAME = "DBZ_REGISTRY_USERNAME";
    private static final String ENV_PASSWORD = "DBZ_REGISTRY_PASSWORD";

    private final Configuration configuration;
    private final DockerRegistryAuthResolver dockerResolver;

    public DefaultRegistryAuthResolver(Configuration configuration,
                                       DockerRegistryAuthResolver dockerResolver) {
        this.configuration = configuration;
        this.dockerResolver = dockerResolver;
    }

    @Override
    public Optional<RegistryAuth> resolve(String registry) {
        // 1. Environment variables
        String envUser = System.getenv(ENV_USERNAME);
        String envPass = System.getenv(ENV_PASSWORD);
        if (envUser != null && !envUser.isBlank() && envPass != null && !envPass.isBlank()) {
            return Optional.of(new RegistryAuth(envUser, envPass));
        }

        if (configuration.getBuild() != null && configuration.getBuild().getContainer() != null) {
            Registry reg = configuration.getBuild().getContainer().getRegistry();
            if (reg != null
                    && reg.getUsername() != null && !reg.getUsername().isBlank()
                    && reg.getPassword() != null && !reg.getPassword().isBlank()) {
                return Optional.of(new RegistryAuth(reg.getUsername(), reg.getPassword()));
            }
        }

        return dockerResolver.resolve(registry);
    }

}
