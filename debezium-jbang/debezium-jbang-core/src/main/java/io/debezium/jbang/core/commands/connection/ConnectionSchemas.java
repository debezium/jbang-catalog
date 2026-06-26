/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.connection.service.ConnectionService;

import picocli.CommandLine;

@CommandLine.Command(name = "schemas", description = "List available connection schemas", sortOptions = false)
public class ConnectionSchemas extends DebeziumCommand {

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public ConnectionSchemas(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        ConnectionService connectionService = platformFactory.connection();
        println(connectionService.connectionSchemas());
        return 0;
    }
}
