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

class ConnectionDeleteJB extends AbstractConnectionJB {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should delete connection by id and confirm")
    void shouldDeleteConnection() throws IOException {
        Path yaml = tempDir.resolve("connection.yaml");
        Files.writeString(yaml, connectionYaml("delete-test-connection"));
        String createOutput = executeConnection("create -f " + yaml.toAbsolutePath());
        String id = createOutput.replace("Connection created with id:", "").trim();

        String output = executeConnection("delete " + id);

        assertThat(output.trim()).isEqualTo("Connection " + id + " deleted.");
    }
}
