/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog.jbang;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogListJB extends AbstractCatalogJB {

    @Test
    @DisplayName("should list catalog components as table")
    void shouldListCatalog() {
        String output = executeCatalog("list");

        assertThat(output).contains("TYPE").contains("CLASS").contains("source");
    }

    @Test
    @DisplayName("should list catalog components filtered by type as table")
    void shouldListCatalogByType() {
        String output = executeCatalog("list --type source");

        assertThat(output).contains("TYPE").contains("CLASS").contains("source");
    }

    @Test
    @DisplayName("should list catalog components as json when --format=json")
    void shouldListCatalogAsJson() {
        String output = executeCatalog("list --format=json");

        assertThat(output.trim()).startsWith("{");
    }
}
