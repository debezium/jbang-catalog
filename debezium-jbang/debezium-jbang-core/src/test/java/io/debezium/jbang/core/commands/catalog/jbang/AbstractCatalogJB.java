/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog.jbang;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import io.debezium.jbang.test.suite.JBangTestPicoCliCommand;

abstract class AbstractCatalogJB extends JBangTestPicoCliCommand {

    static final String MANIFEST_JSON = "{\"components\":{\"source\":{\"io.debezium.connector.postgresql.PostgresConnector\":{}}}}";
    static final String DESCRIPTOR_JSON = "{\"name\":\"Debezium PostgreSQL Connector\",\"type\":\"source-connector\",\"version\":\"3.7.0-SNAPSHOT\"," +
            "\"metadata\":{\"description\":\"Captures row-level changes from a PostgreSQL database in near real-time.\"}," +
            "\"properties\":[" +
            "{\"name\":\"database.hostname\",\"type\":\"string\",\"required\":true," +
            "\"display\":{\"label\":\"Hostname\",\"description\":\"Resolvable hostname or IP address of the database server.\",\"group\":\"Connection\",\"importance\":\"high\"}},"
            +
            "{\"name\":\"database.port\",\"type\":\"number\",\"default\":\"5432\"," +
            "\"display\":{\"label\":\"Port\",\"description\":\"Port of the database server.\",\"group\":\"Connection\",\"importance\":\"high\"}}," +
            "{\"name\":\"database.user\",\"type\":\"string\",\"required\":true," +
            "\"display\":{\"label\":\"User\",\"description\":\"Name of the database user.\",\"group\":\"Connection\",\"importance\":\"high\"}}" +
            "]}";

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
        seedDescriptors();
    }

    private static void seedDescriptors() {
        try {
            String seedScript = "APP_VERSION=$(ls /debezium-platform-conductor/app/*.jar | sed 's/.*conductor-//;s/\\.jar//') && " +
                    "mkdir -p /opt/descriptors/$APP_VERSION/source && " +
                    "printf '%s' '" + MANIFEST_JSON + "' > /opt/descriptors/$APP_VERSION/manifest.json && " +
                    "printf '%s' '" + DESCRIPTOR_JSON + "' > /opt/descriptors/$APP_VERSION/source/io.debezium.connector.postgresql.PostgresConnector.json";
            var result = CONDUCTOR.execInContainer("sh", "-c", seedScript);
            if (result.getExitCode() != 0) {
                throw new RuntimeException("Seed script failed (exit " + result.getExitCode() + "): " + result.getStderr());
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to seed catalog descriptor files into conductor container", e);
        }
    }

    protected String apiUrl() {
        return "http://localhost:" + CONDUCTOR.getMappedPort(8080);
    }

    protected String executeCatalog(String subCommand) {
        return execute("catalog " + subCommand + " --platform-address " + apiUrl());
    }
}
