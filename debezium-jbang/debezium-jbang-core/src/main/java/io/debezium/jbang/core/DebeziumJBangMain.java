/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core;

import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.debezium.jbang.core.commands.pipeline.PipelineCommand;
import io.debezium.jbang.core.commands.pipeline.PipelineCreate;
import io.debezium.jbang.core.commands.pipeline.PipelineDelete;
import io.debezium.jbang.core.commands.pipeline.PipelineGet;
import io.debezium.jbang.core.commands.pipeline.PipelineList;
import io.debezium.jbang.core.commands.pipeline.PipelineUpdate;
import io.debezium.jbang.core.commands.version.DebeziumVersionProvider;
import io.debezium.jbang.core.common.Printer;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import picocli.CommandLine;

@QuarkusMain
@CommandLine.Command(name = "debezium", description = "Debezium CLI", mixinStandardHelpOptions = true)
public class DebeziumJBangMain implements Callable<Integer>, QuarkusApplication {

    private static CommandLine commandLine;

    @Inject
    CommandLine.IFactory factory;

    private Printer out = new Printer.SystemOutPrinter();

    public int run(String... args) {
        try {
            long pid = ProcessHandle.current().pid();
            System.setProperty("pid", Long.toString(pid));
        }
        catch (Exception e) {
            // ignore
        }

        commandLine = new CommandLine(this, factory)
                .addSubcommand("pipeline", new CommandLine(new PipelineCommand(this))
                        .addSubcommand("list", new CommandLine(new PipelineList(this)))
                        .addSubcommand("get", new CommandLine(new PipelineGet(this)))
                        .addSubcommand("create", new CommandLine(new PipelineCreate(this)))
                        .addSubcommand("update", new CommandLine(new PipelineUpdate(this)))
                        .addSubcommand("delete", new CommandLine(new PipelineDelete(this))));

        commandLine.getCommandSpec().versionProvider(new DebeziumVersionProvider());

        return commandLine.execute(args);
    }

    @Override
    public Integer call() {
        commandLine.execute("--help");
        return 0;
    }

    public Printer getOut() {
        return out;
    }
}
