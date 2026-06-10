/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PipelineListIT extends AbstractPipelineIT {

    @Test
    @DisplayName("should list pipelines in table format")
    void shouldListPipelines() {
        mockServer
                .when(request().withMethod("GET").withPath("/pipelines"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":1,\"name\":\"my-pipeline\",\"logLevel\":\"INFO\"}]"));

        String output = executePipeline("list");

        assertThat(output)
                .contains("my-pipeline")
                .contains("INFO");
    }

    @Test
    @DisplayName("should show empty message when no pipelines exist")
    void shouldShowEmptyMessage() {
        mockServer
                .when(request().withMethod("GET").withPath("/pipelines"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]"));

        String output = executePipeline("list");

        assertThat(output.trim()).isEqualTo("No pipelines found.");
    }
}
