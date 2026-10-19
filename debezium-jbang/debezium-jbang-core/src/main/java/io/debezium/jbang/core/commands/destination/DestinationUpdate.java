/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.destination;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.destination.Destination;
import io.debezium.jbang.core.platform.destination.dto.DestinationRequest;
import io.debezium.jbang.core.platform.destination.mapper.DestinationMapper;
import io.debezium.jbang.core.platform.destination.service.DestinationService;

import picocli.CommandLine;

@CommandLine.Command(name = "update", description = "Update an existing destination from a YAML file", sortOptions = false)
public class DestinationUpdate extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Destination id")
    Long id;

    @CommandLine.Option(names = { "--file", "-f" }, required = true, description = "Path to destination YAML file")
    File file;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public DestinationUpdate(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        DestinationRequest request = yamlMapper.readValue(file, DestinationRequest.class);

        DestinationService destinationService = platformFactory.destination(configuration());
        Destination updated = DestinationMapper.toDomain(destinationService.updateDestination(id, request));

        println("Destination " + updated.id() + " updated.");
        return 0;
    }
}
