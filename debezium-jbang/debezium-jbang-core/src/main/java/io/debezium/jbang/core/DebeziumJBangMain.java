/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core;

import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.debezium.jbang.core.commands.connection.ConnectionCollections;
import io.debezium.jbang.core.commands.connection.ConnectionCommand;
import io.debezium.jbang.core.commands.connection.ConnectionCreate;
import io.debezium.jbang.core.commands.connection.ConnectionDelete;
import io.debezium.jbang.core.commands.connection.ConnectionGet;
import io.debezium.jbang.core.commands.connection.ConnectionList;
import io.debezium.jbang.core.commands.connection.ConnectionSchemas;
import io.debezium.jbang.core.commands.connection.ConnectionUpdate;
import io.debezium.jbang.core.commands.connection.ConnectionValidate;
import io.debezium.jbang.core.commands.destination.DestinationCommand;
import io.debezium.jbang.core.commands.destination.DestinationCreate;
import io.debezium.jbang.core.commands.destination.DestinationDelete;
import io.debezium.jbang.core.commands.destination.DestinationGet;
import io.debezium.jbang.core.commands.destination.DestinationList;
import io.debezium.jbang.core.commands.destination.DestinationUpdate;
import io.debezium.jbang.core.commands.pipeline.PipelineCommand;
import io.debezium.jbang.core.commands.pipeline.PipelineCreate;
import io.debezium.jbang.core.commands.pipeline.PipelineDelete;
import io.debezium.jbang.core.commands.pipeline.PipelineGet;
import io.debezium.jbang.core.commands.pipeline.PipelineList;
import io.debezium.jbang.core.commands.pipeline.PipelineUpdate;
import io.debezium.jbang.core.commands.source.SourceCommand;
import io.debezium.jbang.core.commands.source.SourceCreate;
import io.debezium.jbang.core.commands.source.SourceDelete;
import io.debezium.jbang.core.commands.source.SourceGet;
import io.debezium.jbang.core.commands.source.SourceList;
import io.debezium.jbang.core.commands.source.SourceUpdate;
import io.debezium.jbang.core.commands.transform.TransformCommand;
import io.debezium.jbang.core.commands.transform.TransformCreate;
import io.debezium.jbang.core.commands.transform.TransformDelete;
import io.debezium.jbang.core.commands.transform.TransformGet;
import io.debezium.jbang.core.commands.transform.TransformList;
import io.debezium.jbang.core.commands.transform.TransformUpdate;
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
                        .addSubcommand("delete", new CommandLine(new PipelineDelete(this))))
                .addSubcommand("source", new CommandLine(new SourceCommand(this))
                        .addSubcommand("list", new CommandLine(new SourceList(this)))
                        .addSubcommand("get", new CommandLine(new SourceGet(this)))
                        .addSubcommand("create", new CommandLine(new SourceCreate(this)))
                        .addSubcommand("update", new CommandLine(new SourceUpdate(this)))
                        .addSubcommand("delete", new CommandLine(new SourceDelete(this))))
                .addSubcommand("destination", new CommandLine(new DestinationCommand(this))
                        .addSubcommand("list", new CommandLine(new DestinationList(this)))
                        .addSubcommand("get", new CommandLine(new DestinationGet(this)))
                        .addSubcommand("create", new CommandLine(new DestinationCreate(this)))
                        .addSubcommand("update", new CommandLine(new DestinationUpdate(this)))
                        .addSubcommand("delete", new CommandLine(new DestinationDelete(this))))
                .addSubcommand("connection", new CommandLine(new ConnectionCommand(this))
                        .addSubcommand("list", new CommandLine(new ConnectionList(this)))
                        .addSubcommand("get", new CommandLine(new ConnectionGet(this)))
                        .addSubcommand("create", new CommandLine(new ConnectionCreate(this)))
                        .addSubcommand("update", new CommandLine(new ConnectionUpdate(this)))
                        .addSubcommand("delete", new CommandLine(new ConnectionDelete(this)))
                        .addSubcommand("validate", new CommandLine(new ConnectionValidate(this)))
                        .addSubcommand("schemas", new CommandLine(new ConnectionSchemas(this)))
                        .addSubcommand("collections", new CommandLine(new ConnectionCollections(this))))
                .addSubcommand("transform", new CommandLine(new TransformCommand(this))
                        .addSubcommand("list", new CommandLine(new TransformList(this)))
                        .addSubcommand("get", new CommandLine(new TransformGet(this)))
                        .addSubcommand("create", new CommandLine(new TransformCreate(this)))
                        .addSubcommand("update", new CommandLine(new TransformUpdate(this)))
                        .addSubcommand("delete", new CommandLine(new TransformDelete(this))));

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
