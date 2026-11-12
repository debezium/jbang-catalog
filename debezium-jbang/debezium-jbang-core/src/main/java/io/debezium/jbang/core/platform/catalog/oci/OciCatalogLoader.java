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

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.Oci;
import io.debezium.jbang.inventory.oci.OciArtifactDownloader;
import io.debezium.jbang.inventory.oci.OrasArtifactDownloader;

/**
 * Reads the component descriptor OCI artifact coordinates from configuration, downloads it into a local
 * directory via an {@link OciArtifactDownloader} and exposes it as an {@link OciCatalogService}.
 *
 * <p>Coordinates come from the {@code oci} block of the configuration, whose default values
 * ({@code quay.io/debezium/debezium-descriptors:nightly}, cache {@code ~/.dbz/oci}) live in
 * {@code config-defaults.yaml}.</p>
 */
public final class OciCatalogLoader {

    private OciCatalogLoader() {
    }

    public static OciCatalogService load(Configuration configuration, Consumer<String> logger) {
        return load(configuration, logger, new OrasArtifactDownloader());
    }

    static OciCatalogService load(Configuration configuration, Consumer<String> logger, OciArtifactDownloader downloader) {
        Oci oci = configuration.getOci();
        String registry = oci.getRegistry();
        String name = oci.getArtifact().getName();
        String tag = oci.getArtifact().getTag();
        Path cacheDir = expandHome(oci.getLocal());

        try {
            logger.accept("Downloading catalog descriptors from " + registry + "/" + name + ":" + tag + " ...");
            downloader.download(registry, name, tag, cacheDir);
        }
        catch (RuntimeException e) {
            if (!isCached(cacheDir)) {
                throw e;
            }
            logger.accept("Could not refresh catalog descriptors (" + e.getMessage() + "); using cached copy at " + cacheDir);
        }

        return new OciCatalogService(cacheDir);
    }

    static Path expandHome(String path) {
        return path.startsWith("~")
                ? Path.of(System.getProperty("user.home") + path.substring(1))
                : Path.of(path);
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
