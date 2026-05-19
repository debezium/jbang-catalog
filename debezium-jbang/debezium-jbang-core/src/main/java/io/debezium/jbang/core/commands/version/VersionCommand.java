/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.version;

import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.DebeziumJBangMain;

import picocli.CommandLine;

@CommandLine.Command(name = "version", description = "Manage Debezium versions (use version --help to see sub commands)")
public class VersionCommand extends DebeziumCommand {

    public VersionCommand(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {

        new CommandLine(new VersionGet(getMain())).execute();

        return 0;
    }

}
