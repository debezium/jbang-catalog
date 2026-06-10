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

class PipelineDeleteIT extends AbstractPipelineIT {

    @Test
    @DisplayName("should delete pipeline by id and confirm")
    void shouldDeletePipeline() {
        mockServer
                .when(request().withMethod("DELETE").withPath("/pipelines/2"))
                .respond(response().withStatusCode(204));

        String output = executePipeline("delete 2");

        assertThat(output.trim()).isEqualTo("Pipeline 2 deleted.");
    }
}
