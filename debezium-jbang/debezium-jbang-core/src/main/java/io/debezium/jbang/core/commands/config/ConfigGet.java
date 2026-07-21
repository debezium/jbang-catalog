/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.config;

import java.util.List;
import java.util.Map;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.configuration.Configuration;
import io.debezium.jbang.core.configuration.Environment;
import io.debezium.jbang.core.configuration.Platform;
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

        if (environments != null && !environments.isEmpty()) {
            String active = config.getDefaultEnv();
            printf("%-20s %-40s %s%n", "NAME", "URL", "ACTIVE");
            printf("%-20s %-40s %s%n", "----", "---", "------");
            for (Map.Entry<String, Environment> entry : environments.entrySet()) {
                printf("%-20s %-40s %s%n",
                        entry.getKey(),
                        entry.getValue().getUrl() != null ? entry.getValue().getUrl() : "",
                        entry.getKey().equals(active) ? "*" : "");
            }
            println("Effective platform URL: " + ConfigUtil.getPlatformUrl());
            return 0;
        }

        // Legacy platforms list format
        List<Platform> platforms = config.getPlatforms();
        if (platforms != null && !platforms.isEmpty()) {
            printf("%-20s %-40s %s%n", "NAME", "ADDRESS", "DEFAULT");
            printf("%-20s %-40s %s%n", "----", "-------", "-------");
            for (Platform p : platforms) {
                printf("%-20s %-40s %s%n",
                        p.getName() != null ? p.getName() : "",
                        p.getAddress() != null ? p.getAddress() : "",
                        p.isDefault() ? "*" : "");
            }
            println("Effective platform URL: " + ConfigUtil.getPlatformUrl());
            return 0;
        }

        println("No environments configured.");
        println("Default platform URL: " + ConfigUtil.getPlatformUrl());
        return 0;
    }
}
