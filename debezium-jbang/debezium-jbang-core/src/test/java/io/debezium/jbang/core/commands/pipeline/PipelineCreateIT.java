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

class PipelineCreateIT extends AbstractPipelineIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should create pipeline from YAML file and print its id")
    void shouldCreatePipeline() throws IOException {
        Path yaml = tempDir.resolve("pipeline.yaml");
        Files.writeString(yaml, pipelineYaml("create-test-pipeline"));

        String output = executePipeline("create -f " + yaml.toAbsolutePath());

        assertThat(output.trim()).startsWith("Pipeline created with id:");
    }
}
