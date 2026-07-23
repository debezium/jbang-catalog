/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.init.jbang;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.debezium.jbang.test.suite.JBangTestPicoCliCommand;

class InitCommandJB extends JBangTestPicoCliCommand {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should scaffold postgres-kafka dbz.yaml")
    void shouldInitPostgresKafka() throws IOException {
        Path output = tempDir.resolve("dbz.yaml");
        String result = execute("init --source postgres --sink kafka --output " + output.toAbsolutePath());

        assertThat(result).contains("Created");
        assertThat(Files.exists(output)).isTrue();
        String content = Files.readString(output);
        assertThat(content).contains("type: postgres");
        assertThat(content).contains("type: kafka");
        assertThat(content).contains("version: \"1.0\"");
    }

    @Test
    @DisplayName("should scaffold mysql-kafka dbz.yaml")
    void shouldInitMysqlKafka() throws IOException {
        Path output = tempDir.resolve("dbz.yaml");
        String result = execute("init --source mysql --sink kafka --output " + output.toAbsolutePath());

        assertThat(result).contains("Created");
        String content = Files.readString(output);
        assertThat(content).contains("type: mysql");
    }

    @Test
    @DisplayName("should fail when dbz.yaml exists without --force")
    void shouldFailIfExistsWithoutForce() throws IOException {
        Path output = tempDir.resolve("dbz.yaml");
        Files.writeString(output, "existing content");

        String result = instance().execute("init --source postgres --sink kafka --output " + output.toAbsolutePath(), false, true);

        assertThat(result).contains("already exists");
        assertThat(Files.readString(output)).isEqualTo("existing content");
    }

    @Test
    @DisplayName("should overwrite dbz.yaml when --force is set")
    void shouldOverwriteWithForce() throws IOException {
        Path output = tempDir.resolve("dbz.yaml");
        Files.writeString(output, "old content");

        execute("init --source postgres --sink kafka --output " + output.toAbsolutePath() + " --force");

        assertThat(Files.readString(output)).contains("type: postgres");
    }
}
