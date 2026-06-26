/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection.nativebuild;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import io.debezium.jbang.test.suite.NativeTestPicoCliCommand;

abstract class AbstractConnectionIT extends NativeTestPicoCliCommand {

    static final Network NETWORK = Network.newNetwork();

    static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:15")
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres")
            .withEnv("POSTGRES_DB", "conductor")
            .withEnv("POSTGRES_USER", "conductor")
            .withEnv("POSTGRES_PASSWORD", "conductor")
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2)
                    .withStartupTimeout(Duration.ofSeconds(60)));

    static final GenericContainer<?> CONDUCTOR;

    static {
        POSTGRES.start();
        CONDUCTOR = new GenericContainer<>("quay.io/debezium/platform-conductor:nightly")
                .withNetwork(NETWORK)
                .withEnv("CONDUCTOR_WATCHER_ENABLED", "false")
                .withEnv("QUARKUS_DATASOURCE_JDBC_URL", "jdbc:postgresql://postgres:5432/conductor")
                .withEnv("QUARKUS_DATASOURCE_USERNAME", "conductor")
                .withEnv("QUARKUS_DATASOURCE_PASSWORD", "conductor")
                .withExposedPorts(8080)
                .waitingFor(Wait.forLogMessage(".*started in.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(2)));
        CONDUCTOR.start();
    }

    protected String apiUrl() {
        return "http://localhost:" + CONDUCTOR.getMappedPort(8080);
    }

    protected String executeConnection(String subCommand) {
        return execute("connection " + subCommand + " --platform-address " + apiUrl());
    }

    protected String connectionYaml(String name) {
        return "name: " + name + "\ntype: POSTGRESQL\nconfig:\n  hostname: postgres\n  port: 5432\n  username: conductor\n  password: conductor\n  database: conductor\n";
    }
}
