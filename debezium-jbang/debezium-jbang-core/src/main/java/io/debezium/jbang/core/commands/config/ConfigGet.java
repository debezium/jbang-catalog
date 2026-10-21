/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.config;

import java.util.Map;

import io.debezium.jbang.configuration.Environment;
import io.debezium.jbang.configuration.Platform;
import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Display current CLI configuration", sortOptions = false)
public class ConfigGet extends DebeziumCommand {

    public ConfigGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Platform platform = configuration().getPlatform();

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
            println("Effective platform URL: " + configuration().getResolvedPlatform().toString());
        }
        else {
            println("No environments configured.");
            println("Default platform URL: " + configuration().getResolvedPlatform().toString());
        }

        if (configuration().getBuild() != null) {
            println("");
            println("Build:");
            if (configuration().getBuild().getContainer() != null && configuration().getBuild().getContainer().getBase() != null) {
                println("  container.base.image: " + configuration().getBuild().getContainer().getBase().getImage());
                println("  container.base.tag:   " + configuration().getBuild().getContainer().getBase().getTag());
            }
            if (configuration().getBuild().getMaven() != null) {
                println("  maven.local:          " + configuration().getBuild().getMaven().getLocal());
                println("  maven.remote:         " + configuration().getBuild().getMaven().getRemote());
            }
        }

        if (configuration().getOci() != null) {
            println("");
            println("OCI:");
            println("  local:                " + configuration().getOci().getLocal());
            println("  registry:             " + configuration().getOci().getRegistry());
            if (configuration().getOci().getArtifact() != null) {
                println("  artifact.name:        " + configuration().getOci().getArtifact().getName());
                println("  artifact.tag:         " + configuration().getOci().getArtifact().getTag());
            }
        }

        return 0;
    }
}
