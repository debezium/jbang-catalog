/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.build.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnvironmentVariableResolverTest {
    @Test
    void shouldResolveEnvVars() {
        String input = "user: ${HOME}\npassword: ${UNDEFINED_VAR_XYZ}";
        String resolved = EnvironmentVariableResolver.resolve(input);

        assertThat(resolved).contains("user: " + System.getenv("HOME"));
        assertThat(resolved).contains("password: ${UNDEFINED_VAR_XYZ}");
    }
}
