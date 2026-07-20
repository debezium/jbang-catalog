/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.version;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;

import picocli.CommandLine;

@CommandLine.Command(name = "version", description = "Display Debezium CLI version information", mixinStandardHelpOptions = true, sortOptions = false)
public class VersionCommand extends DebeziumCommand {

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public VersionCommand(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Version version = Version.getVersion();

        if (version.jbang() != null) {
            println("JBang version: " + version.jbang());
        }

        if (version.getCore() != null) {
            println("Debezium CLI version: " + version.getCore());
        }

        String platformVersion = platformFactory.getPlatformVersion();
        if (platformVersion != null) {
            println("Debezium Platform version: " + platformVersion);
        }

        return 0;
    }
}
