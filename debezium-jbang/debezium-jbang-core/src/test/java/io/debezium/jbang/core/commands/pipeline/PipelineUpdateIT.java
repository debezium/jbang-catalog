/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

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
        Path yaml = tempDir.resolve("pipeline.yaml");
        Files.writeString(yaml, "name: updated-pipeline\nlogLevel: DEBUG\n");

        mockServer
                .when(request().withMethod("PUT").withPath("/pipelines/3"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":3,\"name\":\"updated-pipeline\",\"logLevel\":\"DEBUG\"}"));

        String output = executePipeline("update 3 -f " + yaml.toAbsolutePath());

        assertThat(output.trim()).isEqualTo("Pipeline 3 updated.");
    }
}
