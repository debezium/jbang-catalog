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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

import io.debezium.jbang.connectors.ConnectorRegistry;
import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.build.config.DbzConfig;
import io.debezium.jbang.core.build.config.DbzConfigLoader;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;
import io.debezium.jbang.core.platform.catalog.dto.Property;
import io.debezium.jbang.core.platform.catalog.service.CatalogService;

import picocli.CommandLine;

@CommandLine.Command(name = "validate", description = "Validate a dbz.yaml project file", mixinStandardHelpOptions = true, sortOptions = false)
public class ValidateCommand extends DebeziumCommand {

    private static final Set<String> KNOWN_SOURCES = Set.of("postgres", "mysql", "mongodb", "sqlserver", "oracle");
    private static final Set<String> KNOWN_SINKS = Set.of("kafka", "http", "pulsar", "redis");

    private static final Map<String, String> SOURCE_TYPE_TO_CLASS = ConnectorRegistry.load();

    private final PropertyValidator validators = new CompositePropertyValidator(
            new RequiredFieldValidator(),
            new EnumValueValidator());

    @CommandLine.Option(names = { "--config" }, description = "Path to dbz.yaml (default: ./dbz.yaml)", defaultValue = "dbz.yaml")
    String configPath;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

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
            Matcher matcher = DbzConfigLoader.getEnvVarPattern().matcher(line);
            while (matcher.find()) {
                String varName = matcher.group(1);
                if (System.getenv(varName) == null && reportedVars.add(varName)) {
                    errors.add("env: referenced variable ${" + varName + "} is not set in the shell environment");
                }
            }
        }

        // Catalog-based connector config validation (requires conductor to be running)
        String connectorClass = config.source() != null && config.source().type() != null
                ? SOURCE_TYPE_TO_CLASS.get(config.source().type().toLowerCase())
                : null;
        if (errors.isEmpty() && connectorClass != null) {
            try {
                CatalogService catalogService = platformFactory.catalog(configuration());
                ComponentDescriptor descriptor = catalogService.getComponentDescriptor("source-connector", connectorClass);
                Map<String, Object> sourceConfig = config.source().config() != null ? config.source().config() : Map.of();
                for (Property p : Objects.requireNonNullElse(descriptor.properties(), List.<Property> of())) {
                    errors.addAll(validators.validate(p, sourceConfig));
                }
            }
            catch (Exception e) {
                println("Warning: Could not reach the conductor for connector config validation: " + e.getMessage());
                println("  Start the conductor and re-run 'debezium validate' for full config validation.");
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
