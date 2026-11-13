/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.platform.catalog.dto.CatalogManifest;
import io.debezium.jbang.core.platform.catalog.dto.ComponentEntry;
import io.debezium.jbang.core.platform.catalog.oci.OciCatalogLoader;
import io.debezium.jbang.core.platform.catalog.service.CatalogService;

import land.oras.Registry;
import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List available catalog components for the current platform version", sortOptions = false)
public class CatalogList extends DebeziumCommand {

    private final Registry registry;

    @CommandLine.Option(names = { "--type" }, description = "Filter by component type (e.g. source-connector, sink-connector, transformation)")
    String type;

    @CommandLine.Option(names = { "--format" }, description = "Output format: table (default) or json", defaultValue = "table")
    String format;

    public CatalogList(DebeziumJBangMain main, Registry registry) {
        super(main);
        this.registry = registry;
    }

    @Override
    public Integer doCall() throws Exception {
        CatalogService catalogService = OciCatalogLoader.load(configuration(), this::errPrintln, registry);
        String json = catalogService.getCatalog(type);
        if ("json".equalsIgnoreCase(format)) {
            println(json);
            return 0;
        }
        printTable(json);
        return 0;
    }

    private void printTable(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CatalogManifest manifest = mapper.readValue(json, CatalogManifest.class);

        List<String[]> rows = new ArrayList<>();
        if (manifest.components() != null) {
            for (Map.Entry<String, List<ComponentEntry>> typeEntry : manifest.components().entrySet()) {
                if (typeEntry.getValue() == null) {
                    continue;
                }
                for (ComponentEntry component : typeEntry.getValue()) {
                    rows.add(new String[]{ typeEntry.getKey(), component.componentClass() != null ? component.componentClass() : "" });
                }
            }
        }

        int typeWidth = Math.max(4, rows.stream().mapToInt(r -> r[0].length()).max().orElse(4));
        int classWidth = Math.max(5, rows.stream().mapToInt(r -> r[1].length()).max().orElse(5));

        String fmt = "%-" + typeWidth + "s  %-" + classWidth + "s%n";
        printf(fmt, "TYPE", "CLASS");
        printf(fmt, "-".repeat(typeWidth), "-".repeat(classWidth));
        for (String[] row : rows) {
            printf(fmt, row[0], row[1]);
        }
    }
}
