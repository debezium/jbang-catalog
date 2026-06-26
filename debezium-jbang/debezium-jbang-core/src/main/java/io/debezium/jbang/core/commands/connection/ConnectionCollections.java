/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.connection.dto.CollectionTree;
import io.debezium.jbang.core.platform.connection.service.ConnectionService;

import picocli.CommandLine;

@CommandLine.Command(name = "collections", description = "List available collections for a connection", sortOptions = false)
public class ConnectionCollections extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Connection id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public ConnectionCollections(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        ConnectionService connectionService = platformFactory.connection();
        CollectionTree tree = connectionService.listAvailableCollections(id);

        if (tree.catalogs() == null || tree.catalogs().isEmpty()) {
            println("No collections found.");
            return 0;
        }

        for (CollectionTree.CatalogNode catalog : tree.catalogs()) {
            if (catalog.name() != null) {
                println(catalog.name());
            }
            if (catalog.schemas() != null) {
                for (CollectionTree.SchemaNode schema : catalog.schemas()) {
                    println("  " + schema.name());
                    if (schema.collections() != null) {
                        for (CollectionTree.CollectionNode collection : schema.collections()) {
                            println("    " + collection.fullyQualifiedName());
                        }
                    }
                }
            }
        }
        return 0;
    }
}
