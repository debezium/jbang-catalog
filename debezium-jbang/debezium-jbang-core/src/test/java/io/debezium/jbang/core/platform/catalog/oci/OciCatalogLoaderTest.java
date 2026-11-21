/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.oci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.debezium.jbang.configuration.Artifact;
import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.Oci;
import io.debezium.jbang.inventory.oci.OciArtifactDownloader;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

class OciCatalogLoaderTest {

    @TempDir
    Path cacheRoot;

    private final List<String> logs = new ArrayList<>();
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldReturnServiceReadingPulledCatalog() {
        OciCatalogLoader underTest = new OciCatalogLoader(configuration("quay.io",
                "debezium/debezium-descriptors",
                "nightly"), logs::add, MAPPER, recordingDownloader());

        OciCatalogHandler service = underTest.load();

        assertThat(service.getCatalog("3.8.0.Final").components()).isNotEmpty();
    }

    @Test
    void shouldFallBackToCacheWhenPullFailsButCacheExists() {
        Configuration configuration = configuration("quay.io", "debezium/debezium-descriptors", "nightly");
        OciCatalogLoader ociCatalogLoader = new OciCatalogLoader(configuration, logs::add, MAPPER, recordingDownloader());

        ociCatalogLoader.load();

        OciCatalogHandler service = new OciCatalogLoader(configuration, logs::add, MAPPER, (r, repo, ref, dir) -> {
            throw new RuntimeException("network down");
        }).load();

        assertThat(logs).anyMatch(l -> l.contains("using cached copy"));
    }

    @Test
    void shouldPropagatePullFailureWhenCacheEmpty() {
        OciArtifactDownloader failing = (r, repo, ref, dir) -> {
            throw new RuntimeException("network down");
        };

        OciCatalogLoader underTest = new OciCatalogLoader(configuration("quay.io",
                "debezium/debezium-descriptors",
                "nightly"), logs::add, MAPPER, failing);

        assertThatThrownBy(underTest::load)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("network down");
    }

    @Test
    void configDefaultsShipDescriptorCoordinates() throws IOException {
        ObjectMapper mapper = new YAMLMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config-defaults.yaml")) {
            Configuration defaults = mapper.readValue(is, Configuration.class);
            Oci oci = defaults.getOci();
            assertThat(oci.getRegistry()).isEqualTo("quay.io");
            assertThat(oci.getLocal()).isEqualTo("~/.dbz/oci");
            assertThat(oci.getArtifact().getName()).isEqualTo("debezium/debezium-descriptors");
            assertThat(oci.getArtifact().getTag()).isEqualTo("nightly");
        }
    }

    private OciArtifactDownloader recordingDownloader() {
        return (registry, repository, reference, targetDir) -> {
            try {
                Path versionDir = targetDir.resolve("3.8.0.Final");
                Files.createDirectories(versionDir);
                Files.writeString(versionDir.resolve("manifest.json"), """
                        {
                          "schemaVersion": 1,
                          "build": {
                            "version": "3.8.0.Final"
                          },
                          "components": {
                            "source-connector": [
                              {
                                "class": "io.debezium.connector.postgresql.PostgresConnector",
                                "name": "Debezium PostgreSQL Connector",
                                "description": "Captures row-level changes from a PostgreSQL database in near real-time.",
                                "descriptor": "source-connector/io.debezium.connector.postgresql.PostgresConnector.json"
                              }
                            ]
                          }
                        }
                        """);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private Configuration configuration(String registry, String name, String tag) {
        Artifact artifact = new Artifact();
        artifact.setName(name);
        artifact.setTag(tag);
        Oci oci = new Oci();
        oci.setLocal(cacheRoot.toString());
        oci.setRegistry(registry);
        oci.setArtifact(artifact);
        Configuration configuration = new Configuration();
        configuration.setOci(oci);
        return configuration;
    }

}
