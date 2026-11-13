/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog.nativebuild;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;

import io.debezium.jbang.test.suite.NativeTestPicoCliCommand;

abstract class AbstractCatalogIT extends NativeTestPicoCliCommand {

    static final String MANIFEST_JSON = """
            {
              "schemaVersion": 1,
              "build": { "version": "3.8.0.Final" },
              "components": {
                "source-connector": [
                  { "class": "io.debezium.connector.postgresql.PostgresConnector", "name": "Debezium PostgreSQL Connector", "description": "Captures row-level changes from a PostgreSQL database in near real-time.", "descriptor": "source-connector/io.debezium.connector.postgresql.PostgresConnector.json" }
                ]
              }
            }
            """;

    static final String DESCRIPTOR_JSON = """
            {
              "name": "Debezium PostgreSQL Connector",
              "type": "source-connector",
              "version": "3.8.0.Final",
              "metadata": {
                "description": "Captures row-level changes from a PostgreSQL database in near real-time."
              },
              "properties": [
                {
                  "name": "database.hostname",
                  "type": "string",
                  "required": true,
                  "display": {
                    "label": "Hostname",
                    "description": "Resolvable hostname or IP address of the database server.",
                    "group": "Connection",
                    "importance": "high"
                  }
                },
                {
                  "name": "database.port",
                  "type": "number",
                  "default": "5432",
                  "display": {
                    "label": "Port",
                    "description": "Port of the database server.",
                    "group": "Connection",
                    "importance": "high"
                  }
                },
                {
                  "name": "database.user",
                  "type": "string",
                  "required": true,
                  "display": {
                    "label": "User",
                    "description": "Name of the database user.",
                    "group": "Connection",
                    "importance": "high"
                  }
                }
              ]
            }
            """;

    @BeforeAll
    static void seedDescriptors() throws IOException {
        Path cacheRoot = Path.of(System.getProperty("user.home"), ".dbz", "oci", "3.8.0.Final");
        Files.createDirectories(cacheRoot.resolve("source-connector"));
        Files.writeString(cacheRoot.resolve("manifest.json"), MANIFEST_JSON);
        Files.writeString(cacheRoot.resolve("source-connector").resolve("io.debezium.connector.postgresql.PostgresConnector.json"), DESCRIPTOR_JSON);
    }

    protected String executeCatalog(String subCommand) {
        return execute("catalog " + subCommand);
    }
}
