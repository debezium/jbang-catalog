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

public interface PipelineLogsContract {

    String executePipeline(String subCommand);

    String executePipelineWithError(String subCommand);

    String pipelineYaml(String name);

    @Test
    @DisplayName("should return error when pipeline has no running environment")
    default void shouldHandleLogsForPipelineWithoutEnvironment() throws IOException {
        Path tempDir = Files.createTempDirectory("pipeline-logs-test");
        try {
            Path yaml = tempDir.resolve("pipeline.yaml");
            Files.writeString(yaml, pipelineYaml("logs-test-pipeline"));
            String createOutput = executePipeline("create -f " + yaml.toAbsolutePath());
            String id = createOutput.replace("Pipeline created with id:", "").trim();
            String error = executePipelineWithError("logs " + id);
            assertThat(error).isNotEmpty();
        }
        finally {
            Files.deleteIfExists(tempDir.resolve("pipeline.yaml"));
            Files.deleteIfExists(tempDir);
        }
    }
}
