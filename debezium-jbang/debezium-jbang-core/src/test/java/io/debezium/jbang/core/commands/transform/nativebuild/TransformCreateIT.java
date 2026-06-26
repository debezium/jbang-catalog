/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.transform.nativebuild;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransformCreateIT extends AbstractTransformIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should create transform from YAML file and print its id")
    void shouldCreateTransform() throws IOException {
        Path yaml = tempDir.resolve("transform.yaml");
        Files.writeString(yaml, transformYaml("create-test-transform"));

        String output = executeTransform("create -f " + yaml.toAbsolutePath());

        assertThat(output.trim()).startsWith("Transform created with id:");
    }
}
