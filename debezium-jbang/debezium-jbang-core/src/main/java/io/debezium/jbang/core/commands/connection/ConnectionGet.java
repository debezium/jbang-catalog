/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.connection.Connection;
import io.debezium.jbang.core.platform.connection.mapper.ConnectionMapper;
import io.debezium.jbang.core.platform.connection.service.ConnectionService;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Get a connection by id", sortOptions = false)
public class ConnectionGet extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Connection id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public ConnectionGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        ConnectionService connectionService = platformFactory.connection();
        Connection c = ConnectionMapper.toDomain(connectionService.getConnection(id));

        println("""
                ID:   %s
                Name: %s
                Type: %s""".formatted(
                c.id(),
                c.name(),
                c.type() != null ? c.type() : "-"));
        return 0;
    }
}
