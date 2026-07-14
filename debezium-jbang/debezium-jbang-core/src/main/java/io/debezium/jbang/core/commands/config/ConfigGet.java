/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.config;

import java.util.Map;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.configuration.Configuration;
import io.debezium.jbang.core.configuration.Environment;
import io.debezium.jbang.core.util.ConfigUtil;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Display current CLI configuration", sortOptions = false)
public class ConfigGet extends DebeziumCommand {

    public ConfigGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Configuration config = Configuration.load();
        Map<String, Environment> environments = config.getEnvironments();

        if (environments == null || environments.isEmpty()) {
            println("No environments configured.");
            println("Default platform URL: " + ConfigUtil.DEFAULT_PLATFORM_URL);
            return 0;
        }

        String active = config.getDefaultEnv();
        printf("%-20s %-40s %s%n", "NAME", "URL", "ACTIVE");
        printf("%-20s %-40s %s%n", "----", "---", "------");
        for (Map.Entry<String, Environment> entry : environments.entrySet()) {
            printf("%-20s %-40s %s%n",
                    entry.getKey(),
                    entry.getValue().getUrl() != null ? entry.getValue().getUrl() : "",
                    entry.getKey().equals(active) ? "*" : "");
        }
        return 0;
    }
}
