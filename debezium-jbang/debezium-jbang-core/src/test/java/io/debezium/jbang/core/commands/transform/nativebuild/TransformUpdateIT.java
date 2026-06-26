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

class TransformUpdateIT extends AbstractTransformIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should update transform from YAML file and confirm")
    void shouldUpdateTransform() throws IOException {
        Path createYaml = tempDir.resolve("create.yaml");
        Files.writeString(createYaml, transformYaml("update-test-transform"));
        String createOutput = executeTransform("create -f " + createYaml.toAbsolutePath());
        String id = createOutput.replace("Transform created with id:", "").trim();

        Path updateYaml = tempDir.resolve("update.yaml");
        Files.writeString(updateYaml, transformYaml("update-test-transform").replace("io.debezium.transforms.Filter", "io.debezium.transforms.Router"));
        String output = executeTransform("update " + id + " -f " + updateYaml.toAbsolutePath());

        assertThat(output.trim()).isEqualTo("Transform " + id + " updated.");
    }
}
