/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.build.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DbzConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseValidConfig() throws IOException {
        Path file = tempDir.resolve("dbz.yaml");
        Files.writeString(file, """
                version: "1.0"
                name: my-pipeline
                source:
                  type: postgres
                  config:
                    database.hostname: localhost
                sink:
                  type: kafka
                  config:
                    producer.bootstrap.servers: localhost:9092
                build:
                  image:
                    name: debezium-server
                    tag: latest
                    registry: ghcr.io/myorg
                """);

        DbzConfig config = DbzConfigLoader.load(file);

        assertThat(config.version()).isEqualTo("1.0");
        assertThat(config.name()).isEqualTo("my-pipeline");
        assertThat(config.source().type()).isEqualTo("postgres");
        assertThat(config.source().config()).containsEntry("database.hostname", "localhost");
        assertThat(config.sink().type()).isEqualTo("kafka");
        assertThat(config.build().image().name()).isEqualTo("debezium-server");
        assertThat(config.build().image().tag()).isEqualTo("latest");
        assertThat(config.build().image().registry()).isEqualTo("ghcr.io/myorg");
    }

    @Test
    void shouldResolveEnvVars() {
        String input = "user: ${HOME}\npassword: ${UNDEFINED_VAR_XYZ}";
        String resolved = DbzConfigLoader.resolveEnvVars(input);

        assertThat(resolved).contains("user: " + System.getenv("HOME"));
        assertThat(resolved).contains("password: ${UNDEFINED_VAR_XYZ}");
    }

    @Test
    void shouldHandleNullOptionalFields() throws IOException {
        Path file = tempDir.resolve("dbz.yaml");
        Files.writeString(file, """
                version: "1.0"
                name: minimal
                source:
                  type: postgres
                sink:
                  type: kafka
                """);

        DbzConfig config = DbzConfigLoader.load(file);

        assertThat(config.source().config()).isNull();
        assertThat(config.sink().config()).isNull();
        assertThat(config.build()).isNull();
    }

    @Test
    void shouldFailOnInvalidYaml() {
        Path file = tempDir.resolve("bad.yaml");
        assertThatThrownBy(() -> {
            Files.writeString(file, "not: valid: yaml: [[[");
            DbzConfigLoader.load(file);
        }).isInstanceOf(IOException.class);
    }

    @Test
    void shouldLoadPostgresKafkaTemplate() {
        String template = DbzConfigLoader.loadTemplate("postgres", "kafka");
        assertThat(template).isNotNull();
        assertThat(template).contains("type: postgres");
        assertThat(template).contains("type: kafka");
    }

    @Test
    void shouldLoadMysqlKafkaTemplate() {
        String template = DbzConfigLoader.loadTemplate("mysql", "kafka");
        assertThat(template).isNotNull();
        assertThat(template).contains("type: mysql");
    }

    @Test
    void shouldLoadMongodbKafkaTemplate() {
        String template = DbzConfigLoader.loadTemplate("mongodb", "kafka");
        assertThat(template).isNotNull();
        assertThat(template).contains("type: mongodb");
    }

    @Test
    void shouldFallBackToGenericTemplateForUnknownCombination() {
        String template = DbzConfigLoader.loadTemplate("sqlserver", "http");
        assertThat(template).isNotNull();
        assertThat(template).contains("sqlserver");
        assertThat(template).contains("http");
    }
}
