/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.transform.jbang;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransformListJB extends AbstractTransformJB {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should list transforms in table format")
    void shouldListTransforms() throws IOException {
        Path yaml = tempDir.resolve("transform.yaml");
        Files.writeString(yaml, transformYaml("list-test-transform"));
        executeTransform("create -f " + yaml.toAbsolutePath());

        String output = executeTransform("list");

        assertThat(output)
                .contains("list-test-transform")
                .contains("io.debezium.transforms.Filter");
    }
}
