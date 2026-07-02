/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline.nativebuild;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipelineLogsIT extends AbstractPipelineIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should return error when pipeline has no running environment")
    void shouldHandleLogsForPipelineWithoutEnvironment() throws IOException {
        Path yaml = tempDir.resolve("pipeline.yaml");
        Files.writeString(yaml, pipelineYaml("logs-test-pipeline"));
        String createOutput = executePipeline("create -f " + yaml.toAbsolutePath());
        String id = createOutput.replace("Pipeline created with id:", "").trim();

        String error = nativeInstance.execute("pipeline logs " + id + " --platform-address " + apiUrl(), true, true);

        assertThat(error).isNotEmpty();
    }
}
