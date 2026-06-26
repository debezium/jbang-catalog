/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;

import picocli.CommandLine;

@CommandLine.Command(name = "delete", description = "Delete a connection by id", sortOptions = false)
public class ConnectionDelete extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Connection id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public ConnectionDelete(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        var connectionService = platformFactory.connection();
        connectionService.deleteConnection(id);

        println("Connection " + id + " deleted.");
        return 0;
    }
}
