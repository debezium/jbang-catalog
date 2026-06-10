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

class PipelineGetIT extends AbstractPipelineIT {

    @Test
    @DisplayName("should display all pipeline details")
    void shouldGetPipeline() {
        mockServer
                .when(request().withMethod("GET").withPath("/pipelines/1"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"my-pipeline\",\"description\":\"desc\"," +
                                "\"source\":{\"id\":10,\"name\":\"my-source\"}," +
                                "\"destination\":{\"id\":20,\"name\":\"my-dest\"}," +
                                "\"logLevel\":\"INFO\"}"));

        String output = executePipeline("get 1");

        assertThat(output)
                .contains("1")
                .contains("my-pipeline")
                .contains("desc")
                .contains("my-source")
                .contains("my-dest")
                .contains("INFO");
    }
}
