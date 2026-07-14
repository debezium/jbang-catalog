/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.config;

import java.util.LinkedHashMap;
import java.util.Map;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.configuration.Configuration;
import io.debezium.jbang.core.configuration.Environment;

import picocli.CommandLine;

@CommandLine.Command(name = "set", description = "Set a CLI configuration value", sortOptions = false)
public class ConfigSet extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Configuration key (supported: platform-url)")
    String key;

    @CommandLine.Parameters(index = "1", description = "Value to set")
    String value;

    @CommandLine.Option(names = { "--env" }, description = "Environment name (default: 'default')", defaultValue = "default")
    String env;

    public ConfigSet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        if (!"platform-url".equals(key)) {
            println("Unknown configuration key: '" + key + "'. Supported keys: platform-url");
            return 1;
        }

        Configuration config = Configuration.load();
        Map<String, Environment> environments = config.getEnvironments() != null
                ? new LinkedHashMap<>(config.getEnvironments())
                : new LinkedHashMap<>();

        Environment environment = environments.getOrDefault(env, new Environment());
        environment.setUrl(value);
        environments.put(env, environment);
        config.setEnvironments(environments);

        if (config.getDefaultEnv() == null && environments.size() == 1) {
            config.setDefaultEnv(env);
        }

        config.save();
        println("Platform URL for environment '" + env + "' set to: " + value);
        return 0;
    }
}
