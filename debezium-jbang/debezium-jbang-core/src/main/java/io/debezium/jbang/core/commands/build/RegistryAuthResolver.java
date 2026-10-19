/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.Registry;

public class RegistryAuthResolver {

    private static final String ENV_USERNAME = "DBZ_REGISTRY_USERNAME";
    private static final String ENV_PASSWORD = "DBZ_REGISTRY_PASSWORD";
    private static final String DOCKER_HUB_V1 = "https://index.docker.io/v1/";
    private static final String DOCKER_HUB_INDEX = "index.docker.io";
    private static final String DOCKER_HUB = "docker.io";
    private static final String SCHEME_REGEX = "https?://";
    private static final String TRAILING_SLASH_REGEX = "/$";

    public record RegistryAuth(String username, String password) {
    }

    public static Optional<RegistryAuth> resolve(String registry, Configuration configuration) {
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

        // 3. ~/.docker/config.json
        return resolveFromDockerConfig(registry,
                Path.of(System.getProperty("user.home"), ".docker", "config.json"));
    }

    static Optional<RegistryAuth> resolveFromDockerConfig(String registry, Path dockerConfigPath) {
        if (!Files.exists(dockerConfigPath)) {
            return Optional.empty();
        }
        try {
            DockerConfig dockerConfig = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(dockerConfigPath.toFile(), DockerConfig.class);
            if (dockerConfig.auths() == null || dockerConfig.auths().isEmpty()) {
                return Optional.empty();
            }
            String key = matchDockerConfigKey(registry, dockerConfig.auths());
            if (key == null) {
                return Optional.empty();
            }
            DockerAuthEntry entry = dockerConfig.auths().get(key);
            if (entry == null || entry.auth() == null || entry.auth().isBlank()) {
                return Optional.empty();
            }
            String decoded = new String(Base64.getDecoder().decode(entry.auth()));
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return Optional.empty();
            }
            return Optional.of(new RegistryAuth(decoded.substring(0, colon), decoded.substring(colon + 1)));
        }
        catch (IOException e) {
            return Optional.empty();
        }
    }

    private static String matchDockerConfigKey(String registry, Map<String, DockerAuthEntry> auths) {
        if (registry == null || registry.isBlank()) {
            for (String candidate : new String[]{ DOCKER_HUB_V1, DOCKER_HUB_INDEX, DOCKER_HUB }) {
                if (auths.containsKey(candidate)) {
                    return candidate;
                }
            }
            return null;
        }
        if (auths.containsKey(registry)) {
            return registry;
        }
        for (String key : auths.keySet()) {
            String normalizedKey = key.replaceFirst(SCHEME_REGEX, "").replaceAll(TRAILING_SLASH_REGEX, "");
            String normalizedRegistry = registry.replaceAll(TRAILING_SLASH_REGEX, "");
            if (normalizedKey.equals(normalizedRegistry) || normalizedRegistry.startsWith(normalizedKey + "/")) {
                return key;
            }
        }
        return null;
    }

    private record DockerConfig(Map<String, DockerAuthEntry> auths) {
    }

    private record DockerAuthEntry(String auth) {
    }
}
