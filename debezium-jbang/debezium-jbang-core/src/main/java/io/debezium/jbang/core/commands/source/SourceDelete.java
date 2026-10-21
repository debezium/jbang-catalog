/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.source;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;

import picocli.CommandLine;

@CommandLine.Command(name = "delete", description = "Delete a source by id", sortOptions = false)
public class SourceDelete extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Source id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public SourceDelete(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        var sourceService = platformFactory.source(configuration());
        sourceService.deleteSource(id);

        println("Source " + id + " deleted.");
        return 0;
    }
}
