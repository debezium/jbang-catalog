/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.transform;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;

import picocli.CommandLine;

@CommandLine.Command(name = "delete", description = "Delete a transform by id", sortOptions = false)
public class TransformDelete extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Transform id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public TransformDelete(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        var transformService = platformFactory.transform();
        transformService.deleteTransform(id);

        println("Transform " + id + " deleted.");
        return 0;
    }
}
