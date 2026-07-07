/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;
import io.debezium.jbang.core.platform.catalog.dto.Property;
import io.debezium.jbang.core.platform.catalog.service.CatalogService;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Get descriptor for a specific catalog component", sortOptions = false)
public class CatalogGet extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Component type (e.g. source, destination)")
    String type;

    @CommandLine.Parameters(index = "1", description = "Component class (e.g. io.debezium.connector.postgresql.PostgresConnector)")
    String componentClass;

    @CommandLine.Option(names = { "--format" }, description = "Output format: table (default) or json", defaultValue = "table")
    String format;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public CatalogGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        CatalogService catalogService = platformFactory.catalog();
        String json = catalogService.getComponentDescriptor(type, componentClass);
        if ("json".equalsIgnoreCase(format)) {
            println(json);
            return 0;
        }
        printTable(json);
        return 0;
    }

    private void printTable(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ComponentDescriptor descriptor = mapper.readValue(json, ComponentDescriptor.class);

        printf("Name:    %s%n", descriptor.name());
        printf("Type:    %s%n", descriptor.type());
        printf("Version: %s%n", descriptor.version());
        if (descriptor.metadata() != null && descriptor.metadata().description() != null) {
            printf("Description: %s%n", descriptor.metadata().description());
        }
        println();

        List<Property> properties = descriptor.properties();
        if (properties == null || properties.isEmpty()) {
            println("No properties defined.");
            return;
        }

        int nameWidth = Math.max(4, properties.stream().mapToInt(p -> p.name().length()).max().orElse(4));
        int typeWidth = Math.max(4, properties.stream().mapToInt(p -> p.type() != null ? p.type().length() : 0).max().orElse(4));
        int defaultWidth = Math.max(7, properties.stream().mapToInt(p -> p.defaultValue() != null ? p.defaultValue().length() : 0).max().orElse(7));

        String fmt = "%-" + nameWidth + "s  %-" + typeWidth + "s  %-8s  %-" + defaultWidth + "s  %s%n";
        printf(fmt, "NAME", "TYPE", "REQUIRED", "DEFAULT", "DESCRIPTION");
        printf(fmt, "-".repeat(nameWidth), "-".repeat(typeWidth), "--------", "-".repeat(defaultWidth), "-----------");
        for (Property p : properties) {
            String desc = p.display() != null && p.display().description() != null ? p.display().description() : "";
            if (desc.length() > 80) {
                desc = desc.substring(0, 77) + "...";
            }
            printf(fmt,
                    p.name(),
                    p.type() != null ? p.type() : "",
                    Boolean.TRUE.equals(p.required()) ? "true" : "false",
                    p.defaultValue() != null ? p.defaultValue() : "",
                    desc);
        }
    }
}
