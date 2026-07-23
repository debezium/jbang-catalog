/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.init;

import java.nio.file.Files;
import java.nio.file.Path;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.build.config.DbzConfigLoader;
import io.debezium.jbang.core.commands.DebeziumCommand;

import picocli.CommandLine;

@CommandLine.Command(name = "init", description = "Scaffold a dbz.yaml project file for a source-sink combination", mixinStandardHelpOptions = true, sortOptions = false)
public class InitCommand extends DebeziumCommand {

    @CommandLine.Option(names = { "--source" }, required = true, description = "Source connector type (e.g. postgres, mysql, mongodb, sqlserver, oracle)")
    String source;

    @CommandLine.Option(names = { "--sink" }, required = true, description = "Sink type (e.g. kafka, http, pulsar, redis)")
    String sink;

    @CommandLine.Option(names = { "--output" }, description = "Output path for the generated dbz.yaml (default: ./dbz.yaml)", defaultValue = "dbz.yaml")
    String output;

    @CommandLine.Option(names = { "--force" }, description = "Overwrite an existing dbz.yaml")
    boolean force;

    public InitCommand(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Path outputPath = Path.of(output);
        if (Files.exists(outputPath) && !force) {
            println(outputPath.getFileName() + " already exists. Use --force to overwrite.");
            return 1;
        }

        String template = DbzConfigLoader.loadTemplate(source.toLowerCase(), sink.toLowerCase());
        if (template == null) {
            println("No template found for source '" + source + "' and sink '" + sink + "'.");
            println("Supported sources: postgres, mysql, mongodb, sqlserver, oracle");
            println("Supported sinks:   kafka, http, pulsar, redis");
            return 1;
        }

        Files.writeString(outputPath, template);
        println("Created " + outputPath + " for " + source + " → " + sink + ".");
        println("Edit the file to fill in your connection details, then run: debezium validate");
        return 0;
    }
}
