/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.connection.nativebuild;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConnectionSchemasIT extends AbstractConnectionIT {

    @Test
    @DisplayName("should print connection schemas as JSON")
    void shouldListConnectionSchemas() {
        String output = executeConnection("schemas");

        assertThat(output.trim()).isNotEmpty();
    }
}
