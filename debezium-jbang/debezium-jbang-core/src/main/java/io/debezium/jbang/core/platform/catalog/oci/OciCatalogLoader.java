/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.oci;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.Oci;
import io.debezium.jbang.inventory.oci.OciArtifactDownloader;

/**
 * Reads the component descriptor OCI artifact coordinates from configuration, downloads it into a local
 * directory via an {@link OciArtifactDownloader} and exposes it as an {@link OciCatalogHandler}.
 *
 * <p>Coordinates come from the {@code oci} block of the configuration, whose default values
 * ({@code quay.io/debezium/debezium-descriptors:nightly}, cache {@code ~/.dbz/oci}) live in
 * {@code config-defaults.yaml}.</p>
 */
public final class OciCatalogLoader {

    private final Configuration configuration;
    private final Consumer<String> logger;
    private final ObjectMapper objectMapper;
    private final OciArtifactDownloader downloader;

    public OciCatalogLoader(Configuration configuration,
                            Consumer<String> logger,
                            ObjectMapper objectMapper,
                            OciArtifactDownloader downloader) {
        this.configuration = configuration;
        this.logger = logger;
        this.objectMapper = objectMapper;
        this.downloader = downloader;
    }

    public OciCatalogHandler load() {
        Oci oci = configuration.getOci();

        try {
            downloader.download(oci.getRegistry(), oci.getArtifact().getName(), oci.getArtifact().getTag(), oci.getResolvedLocal());
        }
        catch (RuntimeException e) {
            if (!isCached(oci.getResolvedLocal())) {
                throw e;
            }
            logger.accept("Could not refresh catalog descriptors (" + e.getMessage() + "); using cached copy at " + oci.getResolvedLocal());
        }

        return new OciCatalogHandler(oci.getResolvedLocal(), objectMapper);
    }

    private static boolean isCached(Path cacheDir) {
        if (!Files.isDirectory(cacheDir)) {
            return false;
        }
        try (var entries = Files.list(cacheDir)) {
            return entries.findAny().isPresent();
        }
        catch (IOException e) {
            return false;
        }
    }
}
