/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog.nativebuild;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogListIT extends AbstractCatalogIT {

    @Test
    @DisplayName("should list catalog components as JSON")
    void shouldListCatalog() {
        String output = executeCatalog("list");

        assertThat(output.trim()).isNotEmpty();
    }

    @Test
    @DisplayName("should list catalog components filtered by type as JSON")
    void shouldListCatalogByType() {
        String output = executeCatalog("list --type source");

        assertThat(output.trim()).isNotEmpty();
    }
}
