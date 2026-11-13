/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.oci;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.debezium.jbang.configuration.Artifact;
import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.Oci;
import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;

import land.oras.Registry;

/**
 * Network integration test that pulls the real descriptor artifact from quay.io. Gated by
 * {@code -DskipNetworkITs=false} (see failsafe config) because it requires outbound network access.
 */
class OciCatalogNetworkIT {

    @TempDir
    Path cacheRoot;

    private Configuration configuration() {
        Artifact artifact = new Artifact();
        artifact.setName("debezium/debezium-descriptors");
        artifact.setTag("nightly");
        Oci oci = new Oci();
        oci.setLocal(cacheRoot.toString());
        oci.setRegistry("quay.io");
        oci.setArtifact(artifact);
        Configuration configuration = new Configuration();
        configuration.setOci(oci);
        return configuration;
    }

    @Test
    void shouldPullDescriptorsFromQuayAndReadPostgresDescriptor() {
        OciCatalogService service = OciCatalogLoader.load(configuration(), System.out::println, Registry.builder().defaults().build());

        String catalog = service.getCatalog("source-connector");
        assertThat(catalog).contains("io.debezium.connector.postgresql.PostgresConnector");

        ComponentDescriptor descriptor = service.getComponentDescriptor(
                "source-connector", "io.debezium.connector.postgresql.PostgresConnector");
        assertThat(descriptor.properties()).isNotEmpty();
        assertThat(descriptor.properties())
                .anySatisfy(p -> assertThat(p.name()).isEqualTo("database.hostname"));
    }
}
