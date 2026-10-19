/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands;

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.ConfigurationLoader;
import io.debezium.jbang.configuration.Platform;
import io.debezium.jbang.core.DebeziumJBangMain;

import picocli.CommandLine;

@CommandLine.Command(name = "switch", description = "Switch the active platform environment", sortOptions = false)
public class SwitchCommand extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Environment name to activate")
    String env;

    public SwitchCommand(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Configuration config = configuration();

        if (config.getPlatform() == null || !config.getPlatform().hasEnvironments()) {
            println("No environments configured. Use 'debezium config set platform-url <url> --env <name>' to add one.");
            return 1;
        }

        if (config.getPlatform().findEnvironment(env) == null) {
            println("Environment '" + env + "' not found. Available: " + String.join(", ", config.getPlatform().getEnvironments().keySet()));
            return 1;
        }

        config.getPlatform().setActive(env);
        save(config);
        println("Switched to environment: " + env);
        return 0;
    }
}
