/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import tools.jackson.databind.ObjectMapper;

public class DockerRegistryAuthResolver implements RegistryAuthResolver {

    private static final String DOCKER_HUB_V1 = "https://index.docker.io/v1/";
    private static final String DOCKER_HUB_INDEX = "index.docker.io";
    private static final String DOCKER_HUB = "docker.io";
    private static final String SCHEME_REGEX = "https?://";
    private static final String TRAILING_SLASH_REGEX = "/$";

    private final Path path;
    private final ObjectMapper objectMapper;

    public DockerRegistryAuthResolver(Path path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<RegistryAuth> resolve(String registry) {
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        DockerConfig dockerConfig = objectMapper
                .readValue(path.toFile(), DockerConfig.class);
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
