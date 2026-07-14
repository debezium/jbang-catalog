/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands;

import java.util.Map;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.configuration.Configuration;

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
        Configuration config = Configuration.load();
        Map<String, ?> environments = config.getEnvironments();

        if (environments == null || environments.isEmpty()) {
            println("No environments configured. Use 'debezium config set platform-url <url> --env <name>' to add one.");
            return 1;
        }

        if (!environments.containsKey(env)) {
            println("Environment '" + env + "' not found. Available: " + String.join(", ", environments.keySet()));
            return 1;
        }

        config.setDefaultEnv(env);
        config.save();
        println("Switched to environment: " + env);
        return 0;
    }
}
