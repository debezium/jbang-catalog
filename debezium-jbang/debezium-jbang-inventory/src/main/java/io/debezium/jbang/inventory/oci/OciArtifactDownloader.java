/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.inventory.oci;

import java.nio.file.Path;

/**
 * Downloads an OCI artifact (such as the Debezium component descriptor registry) into a local
 * directory.
 */
public interface OciArtifactDownloader {

    /**
     * Pulls the artifact identified by the given coordinates and extracts its content into
     * {@code targetDir}.
     *
     * @param registry   registry host, e.g. {@code quay.io}
     * @param repository repository path, e.g. {@code debezium/debezium-descriptors}
     * @param reference  tag or digest, e.g. {@code nightly}
     * @param targetDir  directory the artifact content is extracted into
     */
    void download(String registry, String repository, String reference, Path targetDir);
}
