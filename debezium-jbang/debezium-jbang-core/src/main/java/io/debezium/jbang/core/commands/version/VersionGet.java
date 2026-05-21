/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.version;

import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.DebeziumJBangMain;
import io.debezium.jbang.core.util.VersionUtil;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Displays current Debezium version", sortOptions = false, showDefaultValues = true)
public class VersionGet extends DebeziumCommand {

    public VersionGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() {

        String jbangVersion = VersionUtil.getJBangVersion();

        if (jbangVersion != null) {
            println("JBang version: " + jbangVersion);
        }

        String coreVersion = VersionUtil.getCoreVersion();

        if (coreVersion != null) {
            println("Debezium Core version: " + jbangVersion);
        }

        return 0;
    }
}
