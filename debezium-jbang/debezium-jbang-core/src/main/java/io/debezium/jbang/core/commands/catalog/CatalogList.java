/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.catalog.service.CatalogService;

import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List available catalog components for the current platform version", sortOptions = false)
public class CatalogList extends DebeziumCommand {

    @CommandLine.Option(names = { "--type" }, description = "Filter by component type (e.g. source, destination)")
    String type;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public CatalogList(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        CatalogService catalogService = platformFactory.catalog();
        println(catalogService.getCatalog(type));
        return 0;
    }
}
