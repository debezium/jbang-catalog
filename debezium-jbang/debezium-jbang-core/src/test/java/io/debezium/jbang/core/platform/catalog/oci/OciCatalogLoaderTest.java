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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.debezium.jbang.configuration.Artifact;
import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.Oci;
import io.debezium.jbang.inventory.oci.OciArtifactDownloader;

class OciCatalogLoaderTest {

    @TempDir
    Path cacheRoot;

    private final List<String> logs = new ArrayList<>();

    /** Downloader that records its coordinates and lays down a minimal valid catalog instead of hitting a registry. */
    private OciArtifactDownloader recordingDownloader(List<String> coordinates) {
        return (registry, repository, reference, targetDir) -> {
            coordinates.add(registry + "/" + repository + ":" + reference);
            try {
                Path versionDir = targetDir.resolve("3.8.0.Final");
                Files.createDirectories(versionDir);
                Files.writeString(versionDir.resolve("manifest.json"), "{\"components\":{}}");
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

    @Test
    void shouldPullConfiguredCoordinates() {
        List<String> coordinates = new ArrayList<>();

        OciCatalogLoader.load(configuration("ghcr.io", "myorg/descriptors", "1.2.3"), logs::add, recordingDownloader(coordinates));

        assertThat(coordinates).containsExactly("ghcr.io/myorg/descriptors:1.2.3");
    }

    @Test
    void shouldReturnServiceReadingPulledCatalog() {
        OciCatalogService service = OciCatalogLoader.load(
                configuration("quay.io", "debezium/debezium-descriptors", "nightly"), logs::add, recordingDownloader(new ArrayList<>()));

        assertThat(service.version()).isEqualTo("3.8.0.Final");
        assertThat(service.getCatalog(null)).contains("components");
    }

    @Test
    void shouldFallBackToCacheWhenPullFailsButCacheExists() {
        Configuration configuration = configuration("quay.io", "debezium/debezium-descriptors", "nightly");
        // First populate the cache.
        OciCatalogLoader.load(configuration, logs::add, recordingDownloader(new ArrayList<>()));

        OciArtifactDownloader failing = (r, repo, ref, dir) -> {
            throw new RuntimeException("network down");
        };

        OciCatalogService service = OciCatalogLoader.load(configuration, logs::add, failing);

        assertThat(service.version()).isEqualTo("3.8.0.Final");
        assertThat(logs).anyMatch(l -> l.contains("using cached copy"));
    }

    @Test
    void shouldPropagatePullFailureWhenCacheEmpty() {
        OciArtifactDownloader failing = (r, repo, ref, dir) -> {
            throw new RuntimeException("network down");
        };

        assertThatThrownBy(() -> OciCatalogLoader.load(
                configuration("quay.io", "debezium/debezium-descriptors", "nightly"), logs::add, failing))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("network down");
    }

    @Test
    void shouldExpandTildeAgainstUserHome() {
        Path resolved = OciCatalogLoader.expandHome("~/.dbz/oci");
        assertThat(resolved.toString()).isEqualTo(System.getProperty("user.home") + "/.dbz/oci");
    }

    @Test
    void shouldLeaveAbsolutePathUnchanged() {
        Path resolved = OciCatalogLoader.expandHome("/var/cache/dbz");
        assertThat(resolved.toString()).isEqualTo("/var/cache/dbz");
    }

    @Test
    void configDefaultsShipDescriptorCoordinates() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config-defaults.yaml")) {
            Configuration defaults = mapper.readValue(is, Configuration.class);
            Oci oci = defaults.getOci();
            assertThat(oci.getRegistry()).isEqualTo("quay.io");
            assertThat(oci.getLocal()).isEqualTo("~/.dbz/oci");
            assertThat(oci.getArtifact().getName()).isEqualTo("debezium/debezium-descriptors");
            assertThat(oci.getArtifact().getTag()).isEqualTo("nightly");
        }
    }
}
