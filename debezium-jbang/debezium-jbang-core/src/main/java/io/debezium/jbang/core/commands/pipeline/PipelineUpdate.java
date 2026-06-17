/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.platform.pipeline.Pipeline;
import io.debezium.jbang.core.platform.pipeline.dto.PipelineRequest;
import io.debezium.jbang.core.platform.pipeline.mapper.PipelineMapper;
import io.debezium.jbang.core.platform.pipeline.service.PlatformService;

import picocli.CommandLine;

@CommandLine.Command(name = "update", description = "Update an existing pipeline from a YAML file", sortOptions = false)
public class PipelineUpdate extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Pipeline id")
    Long id;

    @CommandLine.Option(names = { "--file", "-f" }, required = true, description = "Path to pipeline YAML file")
    File file;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public PipelineUpdate(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        PipelineRequest request = yamlMapper.readValue(file, PipelineRequest.class);

        PlatformService platformService = platformFactory.create();
        Pipeline updated = PipelineMapper.toDomain(platformService.updatePipeline(id, request));

        println("Pipeline " + updated.id() + " updated.");
        return 0;
    }
}
