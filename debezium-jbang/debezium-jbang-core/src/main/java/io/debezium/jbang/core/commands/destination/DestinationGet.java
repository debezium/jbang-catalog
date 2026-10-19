/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.destination;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.destination.Destination;
import io.debezium.jbang.core.platform.destination.mapper.DestinationMapper;
import io.debezium.jbang.core.platform.destination.service.DestinationService;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Get a destination by id", sortOptions = false)
public class DestinationGet extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Destination id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public DestinationGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        DestinationService destinationService = platformFactory.destination(configuration());
        Destination d = DestinationMapper.toDomain(destinationService.getDestination(id));

        println("""
                ID:          %s
                Name:        %s
                Description: %s
                Type:        %s
                Schema:      %s
                Connection:  %s""".formatted(
                d.id(),
                d.name(),
                d.description() != null ? d.description() : "-",
                d.type() != null ? d.type() : "-",
                d.schema() != null ? d.schema() : "-",
                d.connection() != null ? d.connection().name() + " (id: " + d.connection().id() + ")" : "-"));
        return 0;
    }
}
