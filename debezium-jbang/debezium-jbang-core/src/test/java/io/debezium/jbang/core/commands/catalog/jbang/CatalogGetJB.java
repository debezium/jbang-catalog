/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog.jbang;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogGetJB extends AbstractCatalogJB {

    @Test
    @DisplayName("should get component descriptor as table")
    void shouldGetComponentDescriptor() {
        String output = executeCatalog("get source io.debezium.connector.postgresql.PostgresConnector");

        assertThat(output).contains("FIELD").contains("VALUE").contains("className");
    }

    @Test
    @DisplayName("should get component descriptor as json when --format=json")
    void shouldGetComponentDescriptorAsJson() {
        String output = executeCatalog("get source io.debezium.connector.postgresql.PostgresConnector --format=json");

        assertThat(output.trim()).startsWith("{");
    }
}
