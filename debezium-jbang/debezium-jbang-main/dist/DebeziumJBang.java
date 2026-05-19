///usr/bin/env jbang "$0" "$@" ; exit $?

/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

//JAVA 21+
//REPOS central=https://repo1.maven.org/maven2,apache-snapshot=https://repository.apache.org/content/groups/snapshots/
//JAVA_OPTIONS -Ddebezium.jbang.quarkusVersion=3.33.1.1
//DEPS io.debezium:debezium-bom:${debezium.jbang.version:3.6.0-SNAPSHOT}@pom
//DEPS io.debezium.jbang:debezium-jbang-core:${debezium.jbang.version:3.6.0-SNAPSHOT}

package main;

import io.debezium.jbang.core.commands.DebeziumJBangMain;

/**
 * Main to run DebeziumJBang
 */
public class DebeziumJBang {

    public static void main(String... args) {
        DebeziumJBangMain.run(args);
    }
}
