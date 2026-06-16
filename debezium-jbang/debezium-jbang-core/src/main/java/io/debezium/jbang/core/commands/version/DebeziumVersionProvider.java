/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.version;

import java.util.ArrayList;
import java.util.List;

import io.debezium.jbang.core.util.Version;

import picocli.CommandLine;

public class DebeziumVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        Version version = Version.getVersion();
        List<String> lines = new ArrayList<>();

        if (version.jbang() != null) {
            lines.add("JBang version: " + version.jbang());
        }

        if (version.getCore() != null) {
            lines.add("Debezium Core version: " + version.getCore());
        }

        return lines.toArray(String[]::new);
    }
}
