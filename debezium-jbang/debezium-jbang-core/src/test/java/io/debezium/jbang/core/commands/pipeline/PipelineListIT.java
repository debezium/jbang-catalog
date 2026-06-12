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

class PipelineListIT extends AbstractPipelineIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should list pipelines in table format")
    void shouldListPipelines() throws IOException {
        Path yaml = tempDir.resolve("pipeline.yaml");
        Files.writeString(yaml, pipelineYaml("list-test-pipeline"));
        executePipeline("create -f " + yaml.toAbsolutePath());

        String output = executePipeline("list");

        assertThat(output)
                .contains("list-test-pipeline")
                .contains("INFO");
    }
}
