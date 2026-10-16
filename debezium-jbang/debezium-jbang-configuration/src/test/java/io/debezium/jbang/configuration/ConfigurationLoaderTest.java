/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadDefaultsFromClasspath() {
        Configuration config = ConfigurationLoader.loadDefaults();

        assertThat(config.getPlatform()).isNotNull();
        assertThat(config.getPlatform().getActive()).isEqualTo("default");
        assertThat(config.getPlatform().getEnvironments()).containsKey("default");
        assertThat(config.getPlatform().getEnvironments().get("default").getUrl()).isEqualTo("http://localhost:8080");

        assertThat(config.getBuild()).isNotNull();
        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
        assertThat(config.getBuild().getContainer().getBase().getTag()).isEqualTo("21-jre");
        assertThat(config.getBuild().getContainer().getRegistry()).isNull();
        assertThat(config.getBuild().getMaven().getLocal()).isEqualTo("~/.m2");
        assertThat(config.getBuild().getMaven().getRemote()).isEqualTo("https://repo1.maven.org/maven2");

        assertThat(config.getOci()).isNotNull();
        assertThat(config.getOci().getLocal()).isEqualTo("~/.dbz/oci");
        assertThat(config.getOci().getRegistry()).isEqualTo("quay.io");
        assertThat(config.getOci().getArtifact().getName()).isEqualTo("debezium/debezium-descriptors");
        assertThat(config.getOci().getArtifact().getTag()).isEqualTo("nightly");
    }

    @Test
    void shouldReturnDefaultsWhenNoUserConfigExists() {
        Path nonexistent = tempDir.resolve("nonexistent.yaml");

        Configuration config = ConfigurationLoader.load(nonexistent);

        assertThat(config.getPlatform().getActive()).isEqualTo("default");
        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
        assertThat(config.getOci().getRegistry()).isEqualTo("quay.io");
    }

    @Test
    void shouldDeepMergeUserConfigOverDefaults() throws IOException {
        Path userConfig = tempDir.resolve("config.yaml");
        Files.writeString(userConfig, """
                build:
                  maven:
                    remote: "https://my-mirror.example.com/maven2"
                """);

        Configuration config = ConfigurationLoader.load(userConfig);

        assertThat(config.getBuild().getMaven().getRemote()).isEqualTo("https://my-mirror.example.com/maven2");
        assertThat(config.getBuild().getMaven().getLocal()).isEqualTo("~/.m2");
        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
        assertThat(config.getPlatform().getActive()).isEqualTo("default");
        assertThat(config.getOci().getRegistry()).isEqualTo("quay.io");
    }

    @Test
    void shouldOverridePlatformEnvironment() throws IOException {
        Path userConfig = tempDir.resolve("config.yaml");
        Files.writeString(userConfig, """
                platform:
                  active: staging
                  environments:
                    staging:
                      url: "http://staging:9090"
                """);

        Configuration config = ConfigurationLoader.load(userConfig);

        assertThat(config.getPlatform().getActive()).isEqualTo("staging");
        assertThat(config.getPlatform().getEnvironments()).containsKey("staging");
        assertThat(config.getPlatform().getEnvironments().get("staging").getUrl()).isEqualTo("http://staging:9090");
    }

    @Test
    void shouldSaveAndReload() throws IOException {
        Configuration config = ConfigurationLoader.loadDefaults();
        config.getPlatform().setActive("production");
        config.getOci().setRegistry("ghcr.io");

        ConfigurationLoader.save(config, tempDir);

        Path savedFile = tempDir.resolve("config.yaml");
        assertThat(savedFile).exists();

        Configuration reloaded = ConfigurationLoader.load(savedFile);
        assertThat(reloaded.getPlatform().getActive()).isEqualTo("production");
        assertThat(reloaded.getOci().getRegistry()).isEqualTo("ghcr.io");
        assertThat(reloaded.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
    }

    @Test
    void shouldIgnoreUnknownFieldsInUserConfig() throws IOException {
        Path userConfig = tempDir.resolve("config.yaml");
        Files.writeString(userConfig, """
                baseImage: "old-image:latest"
                mavenCentralUrl: "https://old-url"
                unknownField: true
                platform:
                  active: default
                """);

        Configuration config = ConfigurationLoader.load(userConfig);

        assertThat(config.getPlatform().getActive()).isEqualTo("default");
        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
    }

    @Test
    void shouldHandleMalformedUserConfigGracefully() throws IOException {
        Path userConfig = tempDir.resolve("config.yaml");
        Files.writeString(userConfig, "not: valid: yaml: [[[");

        Configuration config = ConfigurationLoader.load(userConfig);

        assertThat(config.getPlatform()).isNotNull();
        assertThat(config.getPlatform().getActive()).isEqualTo("default");
    }

    @Test
    void shouldOverrideContainerBaseImage() throws IOException {
        Path userConfig = tempDir.resolve("config.yaml");
        Files.writeString(userConfig, """
                build:
                  container:
                    base:
                      image: "amazoncorretto"
                      tag: "21-al2023-jdk"
                """);

        Configuration config = ConfigurationLoader.load(userConfig);

        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("amazoncorretto");
        assertThat(config.getBuild().getContainer().getBase().getTag()).isEqualTo("21-al2023-jdk");
        assertThat(config.getBuild().getMaven().getRemote()).isEqualTo("https://repo1.maven.org/maven2");
    }

    @Test
    void shouldOverrideOciArtifact() throws IOException {
        Path userConfig = tempDir.resolve("config.yaml");
        Files.writeString(userConfig, """
                oci:
                  registry: "ghcr.io"
                  artifact:
                    name: "myorg/custom-descriptors"
                """);

        Configuration config = ConfigurationLoader.load(userConfig);

        assertThat(config.getOci().getRegistry()).isEqualTo("ghcr.io");
        assertThat(config.getOci().getArtifact().getName()).isEqualTo("myorg/custom-descriptors");
        assertThat(config.getOci().getArtifact().getTag()).isEqualTo("nightly");
    }

    @Test
    void shouldPreserveRegistryCredentials() throws IOException {
        Path userConfig = tempDir.resolve("config.yaml");
        Files.writeString(userConfig, """
                build:
                  container:
                    registry:
                      url: "ghcr.io"
                      username: "myuser"
                      password: "mytoken"
                """);

        Configuration config = ConfigurationLoader.load(userConfig);

        Registry registry = config.getBuild().getContainer().getRegistry();
        assertThat(registry).isNotNull();
        assertThat(registry.getUrl()).isEqualTo("ghcr.io");
        assertThat(registry.getUsername()).isEqualTo("myuser");
        assertThat(registry.getPassword()).isEqualTo("mytoken");
    }
}
