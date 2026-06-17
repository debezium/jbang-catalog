/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.version.nativebuild;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.debezium.jbang.test.suite.NativeTestPicoCliCommand;

class VersionCommandIT extends NativeTestPicoCliCommand {

    @Test
    @DisplayName("should return the version for debezium CLI")
    void shouldReturnTheCorrectVersion() throws IOException {
        String version = System.getProperty("project.version");

        Assertions.assertThat(execute("-V").trim())
                .contains("Debezium Core version: " + version);
    }
}
