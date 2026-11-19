/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.oci;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CatalogVersionsTest {

    @Test
    void shouldUseNewestVersionPreferringReleaseOverSnapshot() {
        CatalogVersions underTest = new CatalogVersions(
                List.of("3.7.0.Final",
                        "3.8.0-SNAPSHOT",
                        "3.8.0.Final"));

        assertThat(underTest.latestVersion()).isEqualTo("3.8.0.Final");
    }

    @Test
    void shouldGetInputVersionIfValid() {
        CatalogVersions underTest = new CatalogVersions(
                List.of("3.7.0.Final",
                        "3.8.0-SNAPSHOT",
                        "3.8.0.Final"));

        assertThat(underTest.acceptedVersion("3.7.0.Final")).isEqualTo("3.7.0.Final");
    }

    @Test
    void shouldGetLatestWhenInputVersionIsInvalid() {
        CatalogVersions underTest = new CatalogVersions(
                List.of("3.7.0.Final",
                        "3.8.0-SNAPSHOT",
                        "3.8.0.Final"));

        assertThat(underTest.acceptedVersion("3.6.0.Final")).isEqualTo("3.8.0.Final");
    }

    @Test
    void shouldGetLatestWhenInputVersionIsNull() {
        CatalogVersions underTest = new CatalogVersions(
                List.of("3.7.0.Final",
                        "3.8.0-SNAPSHOT",
                        "3.8.0.Final"));

        assertThat(underTest.acceptedVersion(null)).isEqualTo("3.8.0.Final");
    }
}
