/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import java.util.UUID;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.pipeline.dto.SignalRequest;

import picocli.CommandLine;

@CommandLine.Command(name = "signal", description = "Send a signal to a pipeline", sortOptions = false)
public class PipelineSignal extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Pipeline id")
    Long id;

    @CommandLine.Option(names = { "--type" }, required = true, description = "Signal type (e.g. log, stop, pause, resume)")
    String type;

    @CommandLine.Option(names = { "--data" }, description = "Signal data payload", defaultValue = "")
    String data;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public PipelineSignal(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        String signalId = UUID.randomUUID().toString();
        platformFactory.pipeline().sendSignal(id, new SignalRequest(signalId, type, data, null));
        println("Signal '" + type + "' sent to pipeline " + id + ".");
        return 0;
    }
}
