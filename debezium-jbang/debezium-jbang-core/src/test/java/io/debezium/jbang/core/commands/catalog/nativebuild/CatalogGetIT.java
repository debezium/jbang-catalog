/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.catalog.nativebuild;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogGetIT extends AbstractCatalogIT {

    @Test
    @DisplayName("should get component descriptor as JSON")
    void shouldGetComponentDescriptor() {
        String output = executeCatalog("get source io.debezium.connector.postgresql.PostgresConnector");

        assertThat(output.trim()).isNotEmpty();
    }
}
