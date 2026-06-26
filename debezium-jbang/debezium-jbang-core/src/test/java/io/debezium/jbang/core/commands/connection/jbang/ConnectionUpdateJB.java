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

class ConnectionUpdateJB extends AbstractConnectionJB {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should update connection from YAML file and confirm")
    void shouldUpdateConnection() throws IOException {
        Path createYaml = tempDir.resolve("create.yaml");
        Files.writeString(createYaml, connectionYaml("update-test-connection"));
        String createOutput = executeConnection("create -f " + createYaml.toAbsolutePath());
        String id = createOutput.replace("Connection created with id:", "").trim();

        Path updateYaml = tempDir.resolve("update.yaml");
        Files.writeString(updateYaml, connectionYaml("update-test-connection-renamed"));
        String output = executeConnection("update " + id + " -f " + updateYaml.toAbsolutePath());

        assertThat(output.trim()).isEqualTo("Connection " + id + " updated.");
    }
}
