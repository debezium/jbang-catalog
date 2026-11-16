/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.platform.catalog.Catalog;
import io.debezium.jbang.core.platform.catalog.oci.CatalogVersions;
import io.debezium.jbang.core.platform.catalog.oci.OciCatalogHandler;
import io.debezium.jbang.core.platform.catalog.oci.OciCatalogLoader;
import io.debezium.jbang.inventory.oci.OrasArtifactDownloader;

import land.oras.Registry;
import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List available catalog components for the current platform version", sortOptions = false)
public class CatalogList extends DebeziumCommand {

    private final OciCatalogLoader ociCatalogLoader;

    @CommandLine.Option(names = { "--type" }, description = "Filter by component type (e.g. source-connector, sink-connector, transformation)")
    String type;

    @CommandLine.Option(names = { "--format" }, description = "Output format: table (default) or json", defaultValue = "table")
    String format;

    @CommandLine.Option(names = { "--version" }, description = "specify catalog version")
    String version;

    public CatalogList(DebeziumJBangMain main, Registry registry) {
        super(main);
        this.ociCatalogLoader = new OciCatalogLoader(configuration(),
                this::errPrintln,
                getMain().getJsonMapper(),
                new OrasArtifactDownloader(registry));
    }

    @Override
    public Integer doCall() throws Exception {
        OciCatalogHandler catalogHandler = ociCatalogLoader.load();
        CatalogVersions catalogVersions = CatalogVersions.from(configuration().getOci().getResolvedLocal());

        String acceptedVersion = catalogVersions.acceptedVersion(version);

        if (type != null) {
            Catalog catalog = catalogHandler.getCatalogByComponentType(acceptedVersion, type);
            print(catalog);
            return 0;
        }

        Catalog catalog = catalogHandler.getCatalog(acceptedVersion);
        print(catalog);

        return 0;
    }

    private void print(Catalog catalog) throws JsonProcessingException {
        if ("json".equalsIgnoreCase(format)) {
            println(getMain().getJsonMapper().writeValueAsString(catalog));
            return;
        }

        printTable(catalog);
    }

    private void printTable(Catalog json) {
        List<String[]> rows = json.components()
                .entrySet()
                .stream()
                .flatMap(components -> components
                        .getValue()
                        .stream()
                        .map(component -> new String[]{ components.getKey(),
                                component.componentClass() != null ? component.componentClass() : "" }))
                .toList();

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
