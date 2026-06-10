/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.integration.ClientAndServer;

import io.debezium.jbang.test.suite.JBangTestPicoCliCommand;

abstract class AbstractPipelineIT extends JBangTestPicoCliCommand {

    protected static ClientAndServer mockServer;

    @BeforeAll
    static void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(0);
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null && mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    @BeforeEach
    void resetMockServer() {
        mockServer.reset();
    }

    protected String apiUrl() {
        return "http://localhost:" + mockServer.getPort();
    }

    protected String executePipeline(String subCommand) {
        return execute("pipeline " + subCommand + " --api-url " + apiUrl());
    }
}
