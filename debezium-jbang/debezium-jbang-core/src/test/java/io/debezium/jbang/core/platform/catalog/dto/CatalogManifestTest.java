/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class CatalogManifestTest {

    @Test
    void shouldParseListBasedComponentsFormat() throws Exception {
        String json = """
                {
                  "schemaVersion": 1,
                  "build": { "version": "3.8.0-SNAPSHOT" },
                  "components": {
                    "source-connector": [
                      { "class": "io.debezium.connector.postgresql.PostgresConnector", "name": "Postgres", "description": "pg", "descriptor": "source-connector/io.debezium.connector.postgresql.PostgresConnector.json" }
                    ],
                    "sink-connector": [
                      { "class": "io.debezium.connector.jdbc.JdbcSinkConnector", "name": "JDBC", "descriptor": "x" }
                    ]
                  }
                }
                """;

        CatalogManifest manifest = new ObjectMapper().readValue(json, CatalogManifest.class);

        assertThat(manifest.components()).containsOnlyKeys("source-connector", "sink-connector");
        ComponentEntry postgres = manifest.components().get("source-connector").get(0);
        assertThat(postgres.componentClass()).isEqualTo("io.debezium.connector.postgresql.PostgresConnector");
        assertThat(postgres.name()).isEqualTo("Postgres");
        assertThat(postgres.description()).isEqualTo("pg");
    }
}
