/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.test.suite.JBangTestPicoCliCommand;

abstract class AbstractPipelineIT extends JBangTestPicoCliCommand {

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

    static long SOURCE_ID;
    static long DEST_ID;

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
        SOURCE_ID = createEntity("/api/sources",
                "{\"name\":\"test-source\",\"schema\":\"io.debezium.postgres\",\"type\":\"source\",\"config\":{}}");
        DEST_ID = createEntity("/api/destinations",
                "{\"name\":\"test-destination\",\"schema\":\"io.debezium.kafka\",\"type\":\"destination\",\"config\":{}}");
    }

    private static long createEntity(String path, String json) {
        try {
            String base = "http://localhost:" + CONDUCTOR.getMappedPort(8080);
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return new ObjectMapper().readTree(res.body()).get("id").asLong();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create entity at " + path, e);
        }
    }

    protected String apiUrl() {
        return "http://localhost:" + CONDUCTOR.getMappedPort(8080);
    }

    protected String executePipeline(String subCommand) {
        return execute("pipeline " + subCommand + " --platform-address " + apiUrl());
    }

    protected String pipelineYaml(String name) {
        return "name: " + name + "\nlogLevel: INFO\nsource:\n  id: " + SOURCE_ID + "\ndestination:\n  id: " + DEST_ID + "\n";
    }
}
