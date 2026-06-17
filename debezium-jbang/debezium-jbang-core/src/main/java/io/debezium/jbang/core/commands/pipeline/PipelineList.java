/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import java.util.List;

import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.platform.pipeline.Pipeline;
import io.debezium.jbang.core.platform.pipeline.mapper.PipelineMapper;
import io.debezium.jbang.core.platform.pipeline.service.PlatformService;

import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List all pipelines", sortOptions = false)
public class PipelineList extends DebeziumCommand {

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public PipelineList(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() {
        PlatformService platformService = platformFactory.create();
        List<Pipeline> pipelines = PipelineMapper.toDomain(platformService.listPipelines());

        if (pipelines.isEmpty()) {
            println("No pipelines found.");
            return 0;
        }

        printf("%-5s %-30s %-20s %-20s %-10s%n", "ID", "NAME", "SOURCE", "DESTINATION", "LOG LEVEL");
        printf("%-5s %-30s %-20s %-20s %-10s%n", "---", "----", "------", "-----------", "---------");
        for (Pipeline p : pipelines) {
            printf("%-5s %-30s %-20s %-20s %-10s%n",
                    p.id(),
                    p.name(),
                    p.source() != null ? p.source().name() : "-",
                    p.destination() != null ? p.destination().name() : "-",
                    p.logLevel() != null ? p.logLevel() : "-");
        }
        return 0;
    }
}
