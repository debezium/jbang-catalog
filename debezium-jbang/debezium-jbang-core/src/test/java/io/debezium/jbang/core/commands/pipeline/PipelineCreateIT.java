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

class PipelineCreateIT extends AbstractPipelineIT {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should create pipeline from YAML file and print its id")
    void shouldCreatePipeline() throws IOException {
        Path yaml = tempDir.resolve("pipeline.yaml");
        Files.writeString(yaml, "name: new-pipeline\nlogLevel: INFO\n");

        mockServer
                .when(request().withMethod("POST").withPath("/pipelines"))
                .respond(response()
                        .withStatusCode(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":5,\"name\":\"new-pipeline\",\"logLevel\":\"INFO\"}"));

        String output = executePipeline("create -f " + yaml.toAbsolutePath());

        assertThat(output.trim()).isEqualTo("Pipeline created with id: 5");
    }
}
