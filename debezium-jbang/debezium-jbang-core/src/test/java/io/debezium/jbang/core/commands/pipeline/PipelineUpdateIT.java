/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipelineUpdateIT extends AbstractPipelineIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should update pipeline from YAML file and confirm")
    void shouldUpdatePipeline() throws IOException {
        Path createYaml = tempDir.resolve("create.yaml");
        Files.writeString(createYaml, pipelineYaml("update-test-pipeline"));
        String createOutput = executePipeline("create -f " + createYaml.toAbsolutePath());
        String id = createOutput.replace("Pipeline created with id:", "").trim();

        Path updateYaml = tempDir.resolve("update.yaml");
        Files.writeString(updateYaml, pipelineYaml("update-test-pipeline").replace("logLevel: INFO", "logLevel: DEBUG"));
        String output = executePipeline("update " + id + " -f " + updateYaml.toAbsolutePath());

        assertThat(output.trim()).isEqualTo("Pipeline " + id + " updated.");
    }
}
