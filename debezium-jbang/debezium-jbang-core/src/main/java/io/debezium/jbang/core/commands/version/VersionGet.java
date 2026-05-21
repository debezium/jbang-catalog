/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.version;

import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.DebeziumJBangMain;
import io.debezium.jbang.core.util.Version;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Displays current Debezium version", sortOptions = false, showDefaultValues = true)
public class VersionGet extends DebeziumCommand {

    public VersionGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() {
        Version version = Version.getVersion();

        if (version.jbang() != null) {
            println("JBang version: " + version.jbang());
        }

        if (version.getCore() != null) {
            println("Debezium Core version: " + version.getCore());
        }

        return 0;
    }
}
