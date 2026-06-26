/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection.nativebuild;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConnectionValidateIT extends AbstractConnectionIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should validate a reachable connection as valid")
    void shouldValidateConnection() throws IOException {
        Path yaml = tempDir.resolve("connection.yaml");
        Files.writeString(yaml, connectionYaml("validate-test-connection"));

        String output = executeConnection("validate -f " + yaml.toAbsolutePath());

        assertThat(output.trim()).isEqualTo("Connection is valid.");
    }
}
