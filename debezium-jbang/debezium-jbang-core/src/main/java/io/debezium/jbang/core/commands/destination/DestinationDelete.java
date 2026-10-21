/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.destination;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;

import picocli.CommandLine;

@CommandLine.Command(name = "delete", description = "Delete a destination by id", sortOptions = false)
public class DestinationDelete extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Destination id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public DestinationDelete(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        var destinationService = platformFactory.destination(configuration());
        destinationService.deleteDestination(id);

        println("Destination " + id + " deleted.");
        return 0;
    }
}
