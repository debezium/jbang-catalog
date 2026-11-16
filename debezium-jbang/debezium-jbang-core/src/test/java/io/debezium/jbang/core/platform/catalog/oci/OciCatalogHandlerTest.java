/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.oci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;

class OciCatalogHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path root;

    @Test
    void shouldReturnFullCatalogWhenTypeNull() throws IOException {
        writeManifest("3.8.0.Final");

        var underTest = new OciCatalogHandler(root, MAPPER).getCatalog("3.8.0.Final");

        assertThat(underTest.components().get("source-connector")).isNotEmpty();
        assertThat(underTest.components().get("sink-connector")).isNotEmpty();
    }

    @Test
    void shouldFilterCatalogByType() throws IOException {
        writeManifest("3.8.0.Final");

        var actual = new OciCatalogHandler(root, MAPPER).getCatalogByComponentType("3.8.0.Final", "source-connector");

        assertThat(actual.components().get("source-connector")).isNotEmpty();
        assertThat(actual.components().get("sink-connector")).isNull();
    }

    @Test
    void shouldReadComponentDescriptor() throws IOException {
        writeManifest("3.8.0.Final");
        writeDescriptor("3.8.0.Final", "source-connector", "io.debezium.connector.postgresql.PostgresConnector", "Postgres");

        ComponentDescriptor descriptor = new OciCatalogHandler(root, MAPPER)
                .getComponentDescriptor("3.8.0.Final", "source-connector", "io.debezium.connector.postgresql.PostgresConnector");

        assertThat(descriptor.name()).isEqualTo("Postgres");
        assertThat(descriptor.properties()).hasSize(1);
        assertThat(descriptor.properties().get(0).name()).isEqualTo("database.hostname");
    }

    @Test
    void shouldThrowWhenDescriptorMissing() throws IOException {
        writeManifest("3.8.0.Final");

        assertThatThrownBy(() -> new OciCatalogHandler(root, MAPPER)
                .getComponentDescriptor("3.8.0.Final", "source-connector", "io.debezium.connector.unknown.Nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nope");
    }

    @Test
    void shouldThrowWhenNoVersionsPresent() {
        assertThatThrownBy(() -> new OciCatalogHandler(root, MAPPER).getCatalog("3.8.0.Final"))
                .isInstanceOf(IllegalStateException.class);
    }

    private void writeDescriptor(String version, String type, String componentClass, String name) throws IOException {
        Path dir = root.resolve(version).resolve(type);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(componentClass + ".json"), """
                {
                  "name": "%s",
                  "type": "%s",
                  "version": "%s",
                  "metadata": { "description": "desc" },
                  "properties": [
                    { "name": "database.hostname", "type": "string", "required": true }
                  ]
                }
                """.formatted(name, type, version));
    }

    private void writeManifest(String version) throws IOException {
        Path dir = root.resolve(version);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("manifest.json"),
                """
                        {
                          "schemaVersion": "1.0",
                          "build": {
                            "version": "%s",
                            "timestamp": "2026-11-11T13:57:15Z",
                            "sourceRepository": "debezium/debezium",
                            "sourceCommit": "16a6c5c",
                            "sourceBranch": "main"
                          },
                          "components": {
                            "converter": [
                              {
                                "class": "io.debezium.converters.BinaryDataConverter",
                                "name": "Debezium Binary Data Converter",
                                "description": "",
                                "descriptor": "converter/io.debezium.converters.BinaryDataConverter.json"
                              },
                              {
                                "class": "io.debezium.converters.ByteArrayConverter",
                                "name": "Debezium Byte Array Converter",
                                "description": "",
                                "descriptor": "converter/io.debezium.converters.ByteArrayConverter.json"
                              },
                              {
                                "class": "io.debezium.converters.CloudEventsConverter",
                                "name": "Debezium Cloud Events Converter",
                                "description": "",
                                "descriptor": "converter/io.debezium.converters.CloudEventsConverter.json"
                              }
                            ],
                            "custom-converter": [
                              {
                                "class": "io.debezium.connector.binlog.converters.JdbcSinkDataTypesConverter",
                                "name": "Debezium JDBC Sink Data Types Custom Converter",
                                "description": "",
                                "descriptor": "custom-converter/io.debezium.connector.binlog.converters.JdbcSinkDataTypesConverter.json"
                              },
                              {
                                "class": "io.debezium.connector.binlog.converters.TinyIntOneToBooleanConverter",
                                "name": "Debezium Tiny Int One To Boolean Custom Converter",
                                "description": "",
                                "descriptor": "custom-converter/io.debezium.connector.binlog.converters.TinyIntOneToBooleanConverter.json"
                              }
                            ],
                            "predicate": [
                              {
                                "class": "io.debezium.transforms.predicates.FieldValueMatches",
                                "name": "Debezium Field Value Matches",
                                "description": "",
                                "descriptor": "predicate/io.debezium.transforms.predicates.FieldValueMatches.json"
                              },
                              {
                                "class": "org.apache.kafka.connect.transforms.predicates.HasHeaderKey",
                                "name": "Kafka Has Header Key",
                                "description": "",
                                "descriptor": "predicate/org.apache.kafka.connect.transforms.predicates.HasHeaderKey.json"
                              }
                            ],
                            "server-sink": [
                              {
                                "class": "io.debezium.server.elasticsearch.ElasticsearchChangeConsumer",
                                "name": "Debezium Elasticsearch Server Sink",
                                "description": "",
                                "descriptor": "server-sink/io.debezium.server.elasticsearch.ElasticsearchChangeConsumer.json"
                              },
                              {
                                "class": "io.debezium.server.eventhubs.EventHubsChangeConsumer",
                                "name": "Debezium Event Hubs Server Sink",
                                "description": "",
                                "descriptor": "server-sink/io.debezium.server.eventhubs.EventHubsChangeConsumer.json"
                              }
                            ],
                            "sink-connector": [
                              {
                                "class": "io.debezium.connector.elasticsearch.ElasticsearchSinkConnector",
                                "name": "Debezium Elasticsearch Sink Connector",
                                "description": "Writes change events to Elasticsearch indices.",
                                "descriptor": "sink-connector/io.debezium.connector.elasticsearch.ElasticsearchSinkConnector.json"
                              },
                              {
                                "class": "io.debezium.connector.jdbc.JdbcSinkConnector",
                                "name": "Debezium JDBC Sink Connector",
                                "description": "",
                                "descriptor": "sink-connector/io.debezium.connector.jdbc.JdbcSinkConnector.json"
                              }
                            ],
                            "source-connector": [
                              {
                                "class": "io.debezium.connector.db2.Db2Connector",
                                "name": "Debezium DB2 Connector",
                                "description": "",
                                "descriptor": "source-connector/io.debezium.connector.db2.Db2Connector.json"
                              },
                              {
                                "class": "io.debezium.connector.mariadb.MariaDbConnector",
                                "name": "Debezium MariaDB Connector",
                                "description": "Captures row-level changes from a MariaDB database in near real-time.",
                                "descriptor": "source-connector/io.debezium.connector.mariadb.MariaDbConnector.json"
                              }
                            ],
                            "transformation": [
                              {
                                "class": "io.debezium.ai.docling.FieldToDocling",
                                "name": "Debezium Field To Docling",
                                "description": "",
                                "descriptor": "transformation/io.debezium.ai.docling.FieldToDocling.json"
                              },
                              {
                                "class": "io.debezium.ai.embeddings.FieldToEmbeddingHuggingFace",
                                "name": "Debezium Field To Embedding",
                                "description": "",
                                "descriptor": "transformation/io.debezium.ai.embeddings.FieldToEmbeddingHuggingFace.json"
                              }
                            ]
                          }
                        }
                        """
                        .formatted(version));
    }

}
