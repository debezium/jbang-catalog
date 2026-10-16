/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.config;

import java.util.Map;

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.ConfigurationLoader;
import io.debezium.jbang.configuration.Environment;
import io.debezium.jbang.configuration.Platform;
import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.util.ConfigUtil;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Display current CLI configuration", sortOptions = false)
public class ConfigGet extends DebeziumCommand {

    public ConfigGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Configuration config = ConfigurationLoader.load();
        Platform platform = config.getPlatform();

        if (platform != null && platform.hasEnvironments()) {
            String active = platform.getActive();
            printf("%-20s %-40s %s%n", "NAME", "URL", "ACTIVE");
            printf("%-20s %-40s %s%n", "----", "---", "------");
            for (Map.Entry<String, Environment> env : platform.getEnvironments().entrySet()) {
                printf("%-20s %-40s %s%n",
                        env.getKey(),
                        env.getValue().getUrl() != null ? env.getValue().getUrl() : "",
                        env.getKey().equals(active) ? "*" : "");
            }
            println("Effective platform URL: " + ConfigUtil.getPlatformUrl());
        }
        else {
            println("No environments configured.");
            println("Default platform URL: " + ConfigUtil.getPlatformUrl());
        }

        if (config.getBuild() != null) {
            println("");
            println("Build:");
            if (config.getBuild().getContainer() != null && config.getBuild().getContainer().getBase() != null) {
                println("  container.base.image: " + config.getBuild().getContainer().getBase().getImage());
                println("  container.base.tag:   " + config.getBuild().getContainer().getBase().getTag());
            }
            if (config.getBuild().getMaven() != null) {
                println("  maven.local:          " + config.getBuild().getMaven().getLocal());
                println("  maven.remote:         " + config.getBuild().getMaven().getRemote());
            }
        }

        if (config.getOci() != null) {
            println("");
            println("OCI:");
            println("  local:                " + config.getOci().getLocal());
            println("  registry:             " + config.getOci().getRegistry());
            if (config.getOci().getArtifact() != null) {
                println("  artifact.name:        " + config.getOci().getArtifact().getName());
                println("  artifact.tag:         " + config.getOci().getArtifact().getTag());
            }
        }

        return 0;
    }
}
