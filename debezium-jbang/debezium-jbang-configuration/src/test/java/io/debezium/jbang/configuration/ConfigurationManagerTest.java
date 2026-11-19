/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

class ConfigurationManagerTest {

    @TempDir
    Path tempDir;

    private String originalHome;
    private ConfigurationManager manager;
    private List<String> messages;

    @BeforeEach
    void setUp() {
        originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        ObjectMapper yamlMapper = new ObjectMapper(
                new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        messages = new ArrayList<>();
        manager = new ConfigurationManager(yamlMapper, messages::add);
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalHome);
    }

    @Test
    void shouldLoadDefaultsFromClasspath() {
        Configuration config = manager.load();

        assertThat(config.getPlatform()).isNotNull();
        assertThat(config.getPlatform().getActive()).isEqualTo("default");
        assertThat(config.getPlatform().getEnvironments()).containsKey("default");
        assertThat(config.getPlatform().getEnvironments().get("default").getUrl()).isEqualTo("http://localhost:8080");

        assertThat(config.getBuild()).isNotNull();
        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
        assertThat(config.getBuild().getContainer().getBase().getTag()).isEqualTo("21-jre");
        assertThat(config.getBuild().getContainer().getRegistry()).isNull();
        assertThat(config.getBuild().getMaven().getLocal()).isEqualTo("~/.m2");
        assertThat(config.getBuild().getMaven().getRemote()).isEqualTo("https://repo1.maven.org/maven2/");

        assertThat(config.getOci()).isNotNull();
        assertThat(config.getOci().getLocal()).isEqualTo("~/.dbz/oci");
        assertThat(config.getOci().getRegistry()).isEqualTo("quay.io");
        assertThat(config.getOci().getArtifact().getName()).isEqualTo("debezium/debezium-descriptors");
        assertThat(config.getOci().getArtifact().getTag()).isEqualTo("nightly");

        assertThat(config.getAuthentication()).isNotNull();
        assertThat(config.getAuthentication().getCredentials()).isEqualTo("~/.dbz/credentials.json");
        assertThat(config.getResolvedCredentials()).isEqualTo(tempDir.resolve(".dbz/credentials.json"));
    }

    @Test
    void shouldNotCreateTheCredentialsFileWhenLoading() {
        manager.load();

        assertThat(Files.exists(tempDir.resolve(".dbz/credentials.json"))).isFalse();
    }

    @Test
    void shouldCreateAnOverriddenCredentialsDirectoryWhenLoading() throws IOException {
        writeUserConfig("""
                authentication:
                  credentials: "~/.custom-dbz/auth/credentials.json"
                """);

        Configuration config = manager.load();

        assertThat(config.getAuthentication().getCredentials()).isEqualTo("~/.custom-dbz/auth/credentials.json");
        assertThat(config.getResolvedCredentials()).isEqualTo(tempDir.resolve(".custom-dbz/auth/credentials.json"));
        Path credentialsDirectory = tempDir.resolve(".custom-dbz/auth");
        assertThat(Files.isDirectory(credentialsDirectory)).isTrue();
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(credentialsDirectory))).isEqualTo("rwx------");
    }

    @Test
    void shouldUseTheDefaultCredentialsPathForMissingOrBlankValues() throws IOException {
        List<String> userConfigs = List.of(
                "authentication:\n",
                "authentication:\n  credentials:\n",
                "authentication:\n  credentials: \"\"\n");

        for (String userConfig : userConfigs) {
            writeUserConfig(userConfig);

            assertThat(manager.load().getResolvedCredentials()).isEqualTo(tempDir.resolve(".dbz/credentials.json"));
        }
    }

    @Test
    void shouldNotExpandTildeForAnotherUser() {
        Authentication authentication = new Authentication();
        authentication.setCredentials("~bob/credentials.json");
        Configuration config = new Configuration();
        config.setAuthentication(authentication);

        assertThat(config.getResolvedCredentials()).isEqualTo(Path.of("~bob/credentials.json"));
    }

    @Test
    void shouldWarnWhenTheCredentialsDirectoryCannotBeCreated() throws IOException {
        Path blocked = tempDir.resolve("blocked");
        Files.writeString(blocked, "not a directory");
        Path credentials = blocked.resolve("auth/credentials.json");
        writeUserConfig("authentication:\n  credentials: '" + credentials + "'\n");

        Configuration config = manager.load();

        assertThat(config.getResolvedCredentials()).isEqualTo(credentials);
        assertThat(messages).anyMatch(message -> message.startsWith("Warning: cannot create credentials directory")
                && message.contains(credentials.getParent().toString()));
    }

    @Test
    void shouldWarnOnlyOnceWhenLoadingSeveralTimes() throws IOException {
        Path blocked = tempDir.resolve("blocked");
        Files.writeString(blocked, "not a directory");
        writeUserConfig("authentication:\n  credentials: '" + blocked.resolve("auth/credentials.json") + "'\n");

        manager.load();
        manager.load();
        manager.load();

        assertThat(messages).filteredOn(message -> message.startsWith("Warning: cannot create credentials directory")).hasSize(1);
    }

    @Test
    void shouldReturnDefaultsWhenNoUserConfigExists() {
        Configuration config = manager.load();

        assertThat(config.getPlatform().getActive()).isEqualTo("default");
        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
        assertThat(config.getOci().getRegistry()).isEqualTo("quay.io");
    }

    @Test
    void shouldDeepMergeUserConfigOverDefaults() throws IOException {
        writeUserConfig("""
                build:
                  maven:
                    remote: "https://my-mirror.example.com/maven2"
                """);

        Configuration config = manager.load();

        assertThat(config.getBuild().getMaven().getRemote()).isEqualTo("https://my-mirror.example.com/maven2");
        assertThat(config.getBuild().getMaven().getLocal()).isEqualTo("~/.m2");
        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
        assertThat(config.getPlatform().getActive()).isEqualTo("default");
        assertThat(config.getOci().getRegistry()).isEqualTo("quay.io");
    }

    @Test
    void shouldOverridePlatformEnvironment() throws IOException {
        writeUserConfig("""
                platform:
                  active: staging
                  environments:
                    staging:
                      url: "http://staging:9090"
                """);

        Configuration config = manager.load();

        assertThat(config.getPlatform().getActive()).isEqualTo("staging");
        assertThat(config.getPlatform().getEnvironments()).containsKey("staging");
        assertThat(config.getPlatform().getEnvironments().get("staging").getUrl()).isEqualTo("http://staging:9090");
    }

    @Test
    void shouldSaveAndReload() {
        Configuration config = manager.load();
        config.getPlatform().setActive("production");
        config.getOci().setRegistry("ghcr.io");

        manager.save(config);

        Configuration reloaded = manager.load();

        assertThat(reloaded.getPlatform().getActive()).isEqualTo("production");
        assertThat(reloaded.getOci().getRegistry()).isEqualTo("ghcr.io");
        assertThat(reloaded.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
    }

    @Test
    void shouldIgnoreUnknownFieldsInUserConfig() throws IOException {
        writeUserConfig("""
                baseImage: "old-image:latest"
                mavenCentralUrl: "https://old-url"
                unknownField: true
                platform:
                  active: default
                """);

        Configuration config = manager.load();

        assertThat(config.getPlatform().getActive()).isEqualTo("default");
        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("eclipse-temurin");
    }

    @Test
    void shouldHandleMalformedUserConfigGracefully() throws IOException {
        writeUserConfig("not: valid: yaml: [[[");

        assertThatThrownBy(() -> manager.load())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldOverrideContainerBaseImage() throws IOException {
        writeUserConfig("""
                build:
                  container:
                    base:
                      image: "amazoncorretto"
                      tag: "21-al2023-jdk"
                """);

        Configuration config = manager.load();

        assertThat(config.getBuild().getContainer().getBase().getImage()).isEqualTo("amazoncorretto");
        assertThat(config.getBuild().getContainer().getBase().getTag()).isEqualTo("21-al2023-jdk");
        assertThat(config.getBuild().getMaven().getRemote()).isEqualTo("https://repo1.maven.org/maven2/");
    }

    @Test
    void shouldOverrideOciArtifact() throws IOException {
        writeUserConfig("""
                oci:
                  registry: "ghcr.io"
                  artifact:
                    name: "myorg/custom-descriptors"
                """);

        Configuration config = manager.load();

        assertThat(config.getOci().getRegistry()).isEqualTo("ghcr.io");
        assertThat(config.getOci().getArtifact().getName()).isEqualTo("myorg/custom-descriptors");
        assertThat(config.getOci().getArtifact().getTag()).isEqualTo("nightly");
    }

    @Test
    void shouldPreserveRegistryCredentials() throws IOException {
        writeUserConfig("""
                build:
                  container:
                    registry:
                      url: "ghcr.io"
                      username: "myuser"
                      password: "mytoken"
                """);

        Configuration config = manager.load();

        Registry registry = config.getBuild().getContainer().getRegistry();
        assertThat(registry).isNotNull();
        assertThat(registry.getUrl()).isEqualTo("ghcr.io");
        assertThat(registry.getUsername()).isEqualTo("myuser");
        assertThat(registry.getPassword()).isEqualTo("mytoken");
    }

    private void writeUserConfig(String content) throws IOException {
        Path configDir = tempDir.resolve(".dbz");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("config.yaml"), content);
    }
}