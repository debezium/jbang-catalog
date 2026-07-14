/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.pipeline.service;

import java.util.List;

import io.debezium.jbang.core.platform.pipeline.dto.PipelineRequest;
import io.debezium.jbang.core.platform.pipeline.dto.PipelineResponse;
import io.debezium.jbang.core.platform.pipeline.dto.SignalRequest;

public interface PlatformService {

    List<PipelineResponse> listPipelines();

    PipelineResponse getPipeline(Long id);

    PipelineResponse createPipeline(PipelineRequest pipelineRequest);

    PipelineResponse updatePipeline(Long id, PipelineRequest pipelineRequest);

    void deletePipeline(Long id);

    String getLogs(Long id);

    void sendSignal(Long id, SignalRequest request);
}
