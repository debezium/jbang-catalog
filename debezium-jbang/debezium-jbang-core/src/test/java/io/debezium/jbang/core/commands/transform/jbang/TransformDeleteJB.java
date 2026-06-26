/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.transform.jbang;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransformDeleteJB extends AbstractTransformJB {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should delete transform by id and confirm")
    void shouldDeleteTransform() throws IOException {
        Path yaml = tempDir.resolve("transform.yaml");
        Files.writeString(yaml, transformYaml("delete-test-transform"));
        String createOutput = executeTransform("create -f " + yaml.toAbsolutePath());
        String id = createOutput.replace("Transform created with id:", "").trim();

        String output = executeTransform("delete " + id);

        assertThat(output.trim()).isEqualTo("Transform " + id + " deleted.");
    }
}
