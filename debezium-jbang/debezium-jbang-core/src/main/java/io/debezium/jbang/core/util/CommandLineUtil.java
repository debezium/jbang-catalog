/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandLineUtil {

    private CommandLineUtil() {
    }

    /**
     * Gets the user home directory.
     *
     * @return the user home directory.
     */
    public static Path getHomeDir() {
        String dir = System.getProperty("user.home");
        if (dir == null || dir.isBlank() || dir.startsWith("?")) {
            dir = System.getenv("HOME");
        }

        return Paths.get(dir);
    }
}
