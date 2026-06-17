/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;

import picocli.CommandLine;

@CommandLine.Command(name = "delete", description = "Delete a pipeline by id", sortOptions = false)
public class PipelineDelete extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Pipeline id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public PipelineDelete(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        var platformService = platformFactory.create();
        platformService.deletePipeline(id);

        println("Pipeline " + id + " deleted.");
        return 0;
    }
}
