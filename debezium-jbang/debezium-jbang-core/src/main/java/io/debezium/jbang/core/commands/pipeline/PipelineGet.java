/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.platform.pipeline.Pipeline;
import io.debezium.jbang.core.platform.pipeline.mapper.PipelineMapper;
import io.debezium.jbang.core.platform.pipeline.service.PlatformService;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Get a pipeline by id", sortOptions = false)
public class PipelineGet extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Pipeline id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public PipelineGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        PlatformService platformService = platformFactory.create();
        Pipeline p = PipelineMapper.toDomain(platformService.getPipeline(id));

        StringBuilder transforms = new StringBuilder();
        if (p.transforms() != null && !p.transforms().isEmpty()) {
            transforms.append("\nTransforms:");
            for (Pipeline.NamedRef t : p.transforms()) {
                transforms.append("\n  - ").append(t.name()).append(" (id: ").append(t.id()).append(")");
            }
        }

        println("""
                ID:          %s
                Name:        %s
                Description: %s
                Source:      %s
                Destination: %s%s
                Log Level:   %s""".formatted(
                p.id(),
                p.name(),
                p.description() != null ? p.description() : "-",
                p.source() != null ? p.source().name() + " (id: " + p.source().id() + ")" : "-",
                p.destination() != null ? p.destination().name() + " (id: " + p.destination().id() + ")" : "-",
                transforms,
                p.logLevel() != null ? p.logLevel() : "-"));
        return 0;
    }
}
