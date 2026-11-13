/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.inventory.oci;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import land.oras.ContainerRef;
import land.oras.Registry;

/**
 * {@link OciArtifactDownloader} backed by the ORAS Java SDK, the same mechanism the Debezium Platform
 * conductor uses to pull component descriptors.
 */
public class OrasArtifactDownloader implements OciArtifactDownloader {

    private final Registry registry;

    public OrasArtifactDownloader(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void download(String registry, String repository, String reference, Path targetDir) {
        try {
            Files.createDirectories(targetDir);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to create download directory " + targetDir, e);
        }

        this.registry.pullArtifact(ContainerRef.parse(registry + "/" + repository + ":" + reference), targetDir, true);
    }
}
