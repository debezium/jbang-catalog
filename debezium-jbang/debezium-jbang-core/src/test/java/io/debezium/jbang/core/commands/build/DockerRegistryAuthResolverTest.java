/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.ObjectMapper;

class DockerRegistryAuthResolverTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsEmptyWhenDockerConfigAbsent() {
        var result = new DockerRegistryAuthResolver(
                tempDir.resolve("nonexistent.json"),
                objectMapper).resolve(null);

        assertThat(result).isEmpty();
    }

    @Test
    void resolvesDockerHubCredentialsFromDefaultKey() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("myuser:mypass".getBytes());
        Path config = tempDir.resolve("config.json");
        Files.writeString(config, """
                {
                  "auths": {
                    "https://index.docker.io/v1/": {
                      "auth": "%s"
                    }
                  }
                }
                """.formatted(encoded));

        var result = new DockerRegistryAuthResolver(config, objectMapper).resolve(null);

        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("myuser");
        assertThat(result.get().password()).isEqualTo("mypass");
    }

    @Test
    void resolvesGhcrCredentialsByHostname() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("ghuser:ghtoken".getBytes());
        Path config = tempDir.resolve("config.json");
        Files.writeString(config, """
                {
                  "auths": {
                    "ghcr.io": {
                      "auth": "%s"
                    }
                  }
                }
                """.formatted(encoded));

        var result = new DockerRegistryAuthResolver(config, objectMapper).resolve("ghcr.io");

        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("ghuser");
        assertThat(result.get().password()).isEqualTo("ghtoken");
    }

    @Test
    void returnsEmptyWhenNoMatchingRegistryInDockerConfig() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("user:pass".getBytes());
        Path config = tempDir.resolve("config.json");
        Files.writeString(config, """
                {
                  "auths": {
                    "ghcr.io": {
                      "auth": "%s"
                    }
                  }
                }
                """.formatted(encoded));

        var result = new DockerRegistryAuthResolver(config, objectMapper).resolve("registry.example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenAuthsIsEmpty() throws Exception {
        Path config = tempDir.resolve("config.json");
        Files.writeString(config, """
                {
                  "auths": {}
                }
                """);

        var result = new DockerRegistryAuthResolver(config, objectMapper).resolve(null);

        assertThat(result).isEmpty();
    }

    @Test
    void handlesHttpsPrefixedKeyForCustomRegistry() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        Path config = tempDir.resolve("config.json");
        Files.writeString(config, """
                {
                  "auths": {
                    "https://myregistry.example.com": {
                      "auth": "%s"
                    }
                  }
                }
                """.formatted(encoded));

        var result = new DockerRegistryAuthResolver(config, objectMapper).resolve("myregistry.example.com");

        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("admin");
        assertThat(result.get().password()).isEqualTo("secret");
    }
}
