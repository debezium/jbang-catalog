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

class PipelineGetIT extends AbstractPipelineIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should display all pipeline details")
    void shouldGetPipeline() throws IOException {
        Path yaml = tempDir.resolve("pipeline.yaml");
        Files.writeString(yaml, pipelineYaml("get-test-pipeline"));
        String createOutput = executePipeline("create -f " + yaml.toAbsolutePath());
        String id = createOutput.replace("Pipeline created with id:", "").trim();

        String output = executePipeline("get " + id);

        assertThat(output)
                .contains(id)
                .contains("get-test-pipeline")
                .contains("INFO");
    }
}
