/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.destination;

import java.util.List;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.destination.Destination;
import io.debezium.jbang.core.platform.destination.mapper.DestinationMapper;
import io.debezium.jbang.core.platform.destination.service.DestinationService;

import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List all destinations", sortOptions = false)
public class DestinationList extends DebeziumCommand {

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public DestinationList(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() {
        DestinationService destinationService = platformFactory.destination(configuration());
        List<Destination> destinations = DestinationMapper.toDomain(destinationService.listDestinations());

        if (destinations.isEmpty()) {
            println("No destinations found.");
            return 0;
        }

        printf("%-5s %-30s %-20s %-20s%n", "ID", "NAME", "TYPE", "SCHEMA");
        printf("%-5s %-30s %-20s %-20s%n", "---", "----", "----", "------");
        for (Destination d : destinations) {
            printf("%-5s %-30s %-20s %-20s%n",
                    d.id(),
                    d.name(),
                    d.type() != null ? d.type() : "-",
                    d.schema() != null ? d.schema() : "-");
        }
        return 0;
    }
}
