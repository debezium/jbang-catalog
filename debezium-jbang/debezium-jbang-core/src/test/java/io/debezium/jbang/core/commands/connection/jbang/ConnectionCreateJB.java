/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection.jbang;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConnectionCreateJB extends AbstractConnectionJB {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should create connection from YAML file and print its id")
    void shouldCreateConnection() throws IOException {
        Path yaml = tempDir.resolve("connection.yaml");
        Files.writeString(yaml, connectionYaml("create-test-connection"));

        String output = executeConnection("create -f " + yaml.toAbsolutePath());

        assertThat(output.trim()).startsWith("Connection created with id:");
    }
}
