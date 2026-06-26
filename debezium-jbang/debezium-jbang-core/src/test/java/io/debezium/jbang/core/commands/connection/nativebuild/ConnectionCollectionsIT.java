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

class ConnectionCollectionsIT extends AbstractConnectionIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should list available collections for a reachable connection")
    void shouldListCollections() throws IOException {
        Path yaml = tempDir.resolve("connection.yaml");
        Files.writeString(yaml, connectionYaml("collections-test-connection"));
        String createOutput = executeConnection("create -f " + yaml.toAbsolutePath());
        String id = createOutput.replace("Connection created with id:", "").trim();

        String output = executeConnection("collections " + id);

        assertThat(output.trim()).isNotEmpty();
    }
}
