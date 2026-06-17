/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.DebeziumJBangMain;

import picocli.CommandLine;

@CommandLine.Command(name = "pipeline", description = "Manage Debezium Platform pipelines (use pipeline --help to see subcommands)", mixinStandardHelpOptions = true, sortOptions = false)
public class PipelineCommand extends DebeziumCommand {

    public PipelineCommand(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        spec.commandLine().execute("--help");
        return 0;
    }
}
