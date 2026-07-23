/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.validate.jbang;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.debezium.jbang.test.suite.JBangTestPicoCliCommand;

class ValidateCommandJB extends JBangTestPicoCliCommand {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should pass validation for a valid dbz.yaml")
    void shouldValidateValidConfig() throws IOException {
        Path config = tempDir.resolve("dbz.yaml");
        Files.writeString(config, """
                version: "1.0"
                name: my-pipeline
                source:
                  type: postgres
                sink:
                  type: kafka
                """);

        String result = execute("validate --config " + config.toAbsolutePath());

        assertThat(result).contains("dbz.yaml is valid");
        assertThat(result).contains("source: postgres");
        assertThat(result).contains("sink:   kafka");
    }

    @Test
    @DisplayName("should fail when file does not exist")
    void shouldFailIfFileNotFound() {
        String result = instance().execute("validate --config /nonexistent/dbz.yaml", false, true);

        assertThat(result).contains("not found");
    }

    @Test
    @DisplayName("should fail and report missing required fields")
    void shouldFailOnMissingFields() throws IOException {
        Path config = tempDir.resolve("dbz.yaml");
        Files.writeString(config, """
                version: "1.0"
                source:
                  type: postgres
                """);

        String result = instance().execute("validate --config " + config.toAbsolutePath(), false, true);

        assertThat(result).contains("name: required field is missing");
        assertThat(result).contains("sink: required block is missing");
    }

    @Test
    @DisplayName("should fail on unknown connector type")
    void shouldFailOnUnknownConnector() throws IOException {
        Path config = tempDir.resolve("dbz.yaml");
        Files.writeString(config, """
                version: "1.0"
                name: test
                source:
                  type: unknowndb
                sink:
                  type: kafka
                """);

        String result = instance().execute("validate --config " + config.toAbsolutePath(), false, true);

        assertThat(result).contains("unknown connector");
    }
}
