/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.connection.Connection;
import io.debezium.jbang.core.platform.connection.dto.ConnectionRequest;
import io.debezium.jbang.core.platform.connection.mapper.ConnectionMapper;
import io.debezium.jbang.core.platform.connection.service.ConnectionService;

import picocli.CommandLine;

@CommandLine.Command(name = "create", description = "Create a new connection from a YAML file", sortOptions = false)
public class ConnectionCreate extends DebeziumCommand {

    @CommandLine.Option(names = { "--file", "-f" }, required = true, description = "Path to connection YAML file")
    File file;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public ConnectionCreate(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        ConnectionRequest request = yamlMapper.readValue(file, ConnectionRequest.class);

        ConnectionService connectionService = platformFactory.connection();
        Connection created = ConnectionMapper.toDomain(connectionService.createConnection(request));

        println("Connection created with id: " + created.id());
        return 0;
    }
}
