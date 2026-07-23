/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.validate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.build.config.DbzConfig;
import io.debezium.jbang.core.build.config.DbzConfigLoader;
import io.debezium.jbang.core.commands.DebeziumCommand;

import picocli.CommandLine;

@CommandLine.Command(name = "validate", description = "Validate a dbz.yaml project file", mixinStandardHelpOptions = true, sortOptions = false)
public class ValidateCommand extends DebeziumCommand {

    private static final Set<String> KNOWN_SOURCES = Set.of("postgres", "mysql", "mongodb", "sqlserver", "oracle");
    private static final Set<String> KNOWN_SINKS = Set.of("kafka", "http", "pulsar", "redis");
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @CommandLine.Option(names = { "--config" }, description = "Path to dbz.yaml (default: ./dbz.yaml)", defaultValue = "dbz.yaml")
    String configPath;

    public ValidateCommand(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Path path = Path.of(configPath);

        if (!Files.exists(path)) {
            println("ERROR: " + configPath + " not found. Run 'debezium init --source <type> --sink <type>' to create one.");
            return 1;
        }

        DbzConfig config;
        try {
            config = DbzConfigLoader.load(path);
        }
        catch (Exception e) {
            println("ERROR: Failed to parse " + configPath + ": " + e.getMessage());
            return 1;
        }

        List<String> errors = new ArrayList<>();

        if (config.version() == null || config.version().isBlank()) {
            errors.add("version: required field is missing");
        }
        if (config.name() == null || config.name().isBlank()) {
            errors.add("name: required field is missing");
        }

        if (config.source() == null) {
            errors.add("source: required block is missing");
        }
        else if (config.source().type() == null || config.source().type().isBlank()) {
            errors.add("source.type: required field is missing");
        }
        else if (!KNOWN_SOURCES.contains(config.source().type().toLowerCase())) {
            errors.add("source.type: unknown connector '" + config.source().type() + "'. Known: " + String.join(", ", KNOWN_SOURCES));
        }

        if (config.sink() == null) {
            errors.add("sink: required block is missing");
        }
        else if (config.sink().type() == null || config.sink().type().isBlank()) {
            errors.add("sink.type: required field is missing");
        }
        else if (!KNOWN_SINKS.contains(config.sink().type().toLowerCase())) {
            errors.add("sink.type: unknown sink '" + config.sink().type() + "'. Known: " + String.join(", ", KNOWN_SINKS));
        }

        // Check that all ${VAR} references are set in the shell environment (skip comment lines)
        String rawContent = Files.readString(path);
        Set<String> reportedVars = new java.util.HashSet<>();
        for (String line : rawContent.lines().toList()) {
            if (line.stripLeading().startsWith("#")) {
                continue;
            }
            Matcher matcher = ENV_VAR_PATTERN.matcher(line);
            while (matcher.find()) {
                String varName = matcher.group(1);
                if (System.getenv(varName) == null && reportedVars.add(varName)) {
                    errors.add("env: referenced variable ${" + varName + "} is not set in the shell environment");
                }
            }
        }

        if (!errors.isEmpty()) {
            println("Validation failed with " + errors.size() + " error(s):");
            for (String error : errors) {
                println("  - " + error);
            }
            return 1;
        }

        println("dbz.yaml is valid.");
        println("  source: " + config.source().type());
        println("  sink:   " + config.sink().type());
        if (config.build() != null && config.build().image() != null) {
            DbzConfig.ImageConfig img = config.build().image();
            String imageRef = (img.registry() != null ? img.registry() + "/" : "")
                    + (img.name() != null ? img.name() : "debezium-server")
                    + (img.tag() != null ? ":" + img.tag() : "");
            println("  image:  " + imageRef);
        }
        return 0;
    }
}
