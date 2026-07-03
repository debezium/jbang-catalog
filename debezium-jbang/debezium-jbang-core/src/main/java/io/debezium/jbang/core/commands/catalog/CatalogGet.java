/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
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
        JsonNode root = mapper.readTree(json);

        List<String[]> rows = new ArrayList<>();
        root.fields().forEachRemaining(entry -> rows.add(new String[]{ entry.getKey(), entry.getValue().asText() }));

        int fieldWidth = Math.max(5, rows.stream().mapToInt(r -> r[0].length()).max().orElse(5));
        int valueWidth = Math.max(5, rows.stream().mapToInt(r -> r[1].length()).max().orElse(5));

        String fmt = "%-" + fieldWidth + "s  %-" + valueWidth + "s%n";
        printf(fmt, "FIELD", "VALUE");
        printf(fmt, "-".repeat(fieldWidth), "-".repeat(valueWidth));
        for (String[] row : rows) {
            printf(fmt, row[0], row[1]);
        }
    }
}
