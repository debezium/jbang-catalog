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

@CommandLine.Command(name = "get", description = "Get descriptor for a specific catalog component", sortOptions = false)
public class CatalogGet extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Component type (e.g. source, destination)")
    String type;

    @CommandLine.Parameters(index = "1", description = "Component class (e.g. io.debezium.connector.postgresql.PostgresConnector)")
    String componentClass;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public CatalogGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        CatalogService catalogService = platformFactory.catalog();
        println(catalogService.getComponentDescriptor(type, componentClass));
        return 0;
    }
}
