/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.version;

import io.debezium.jbang.test.suite.JBangTestPicoCliCommand;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VersionCommandTest extends JBangTestPicoCliCommand {

    @Test
    @DisplayName("should return the version for debezium CLI")
    void shouldReturnTheCorrectVersion() {
        Assertions.assertThat(execute("version").trim())
                .containsIgnoringNewLines("""
                        JBang version: %s
                        Debezium Core version: %s
                        """.formatted(instance().getJbangVersion(),
                        instance().getJbangVersion()));
    }
}
