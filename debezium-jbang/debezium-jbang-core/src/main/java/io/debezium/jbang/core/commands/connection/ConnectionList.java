/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection;

import java.util.List;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.connection.Connection;
import io.debezium.jbang.core.platform.connection.mapper.ConnectionMapper;
import io.debezium.jbang.core.platform.connection.service.ConnectionService;

import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List all connections", sortOptions = false)
public class ConnectionList extends DebeziumCommand {

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public ConnectionList(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() {
        ConnectionService connectionService = platformFactory.connection();
        List<Connection> connections = ConnectionMapper.toDomain(connectionService.listConnections());

        if (connections.isEmpty()) {
            println("No connections found.");
            return 0;
        }

        printf("%-5s %-30s %-20s%n", "ID", "NAME", "TYPE");
        printf("%-5s %-30s %-20s%n", "---", "----", "----");
        for (Connection c : connections) {
            printf("%-5s %-30s %-20s%n",
                    c.id(),
                    c.name(),
                    c.type() != null ? c.type() : "-");
        }
        return 0;
    }
}
