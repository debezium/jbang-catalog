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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;

class OciCatalogServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path root;

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
                          "schemaVersion": 1,
                          "build": { "version": "%s" },
                          "components": {
                            "source-connector": [
                              { "class": "io.debezium.connector.postgresql.PostgresConnector", "name": "Postgres", "descriptor": "source-connector/io.debezium.connector.postgresql.PostgresConnector.json" }
                            ],
                            "sink-connector": [
                              { "class": "io.debezium.connector.jdbc.JdbcSinkConnector", "name": "JDBC", "descriptor": "x" }
                            ]
                          }
                        }
                        """
                        .formatted(version));
    }

    @Test
    void shouldUseNewestVersionPreferringReleaseOverSnapshot() throws IOException {
        writeManifest("3.7.0.Final");
        writeManifest("3.8.0-SNAPSHOT");
        writeManifest("3.8.0.Final");

        OciCatalogService service = new OciCatalogService(root);

        assertThat(service.version()).isEqualTo("3.8.0.Final");
    }

    @Test
    void shouldOrderSnapshotBeforeMatchingRelease() {
        assertThat(OciCatalogService.versionComparator().compare("3.8.0-SNAPSHOT", "3.8.0.Final")).isNegative();
        assertThat(OciCatalogService.versionComparator().compare("3.7.0.Final", "3.8.0-SNAPSHOT")).isNegative();
        assertThat(OciCatalogService.versionComparator().compare("3.10.0.Final", "3.9.0.Final")).isPositive();
    }

    @Test
    void shouldReturnFullCatalogWhenTypeNull() throws IOException {
        writeManifest("3.8.0.Final");

        String json = new OciCatalogService(root).getCatalog(null);
        JsonNode node = MAPPER.readTree(json);

        assertThat(node.path("components").has("source-connector")).isTrue();
        assertThat(node.path("components").has("sink-connector")).isTrue();
    }

    @Test
    void shouldFilterCatalogByType() throws IOException {
        writeManifest("3.8.0.Final");

        String json = new OciCatalogService(root).getCatalog("source-connector");
        JsonNode components = MAPPER.readTree(json).path("components");

        assertThat(components.has("source-connector")).isTrue();
        assertThat(components.has("sink-connector")).isFalse();
    }

    @Test
    void shouldReadComponentDescriptor() throws IOException {
        writeManifest("3.8.0.Final");
        writeDescriptor("3.8.0.Final", "source-connector", "io.debezium.connector.postgresql.PostgresConnector", "Postgres");

        ComponentDescriptor descriptor = new OciCatalogService(root)
                .getComponentDescriptor("source-connector", "io.debezium.connector.postgresql.PostgresConnector");

        assertThat(descriptor.name()).isEqualTo("Postgres");
        assertThat(descriptor.properties()).hasSize(1);
        assertThat(descriptor.properties().get(0).name()).isEqualTo("database.hostname");
    }

    @Test
    void shouldThrowWhenDescriptorMissing() throws IOException {
        writeManifest("3.8.0.Final");

        assertThatThrownBy(() -> new OciCatalogService(root)
                .getComponentDescriptor("source-connector", "io.debezium.connector.unknown.Nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nope");
    }

    @Test
    void shouldThrowWhenNoVersionsPresent() {
        assertThatThrownBy(() -> new OciCatalogService(root))
                .isInstanceOf(IllegalStateException.class);
    }
}
