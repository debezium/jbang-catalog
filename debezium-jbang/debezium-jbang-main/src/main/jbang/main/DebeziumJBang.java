///usr/bin/env jbang "$0" "$@" ; exit $?

/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

//JAVA 21+
//REPOS central=https://repo1.maven.org/maven2,apache-snapshot=https://repository.apache.org/content/groups/snapshots/
//JAVA_OPTIONS -Ddebezium.jbang.quarkusVersion=3.33.1.1 --add-opens java.base/java.lang=ALL-UNNAMED
//DEPS io.quarkus.platform:quarkus-bom:3.33.1.1@pom
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager
//DEPS io.debezium:debezium-bom:${debezium.jbang.version:3.6.0-SNAPSHOT}@pom
//DEPS io.debezium.jbang:debezium-jbang-core:${debezium.jbang.version:3.6.0-SNAPSHOT}
//DEPS io.quarkus:quarkus-arc
//DEPS io.quarkus:quarkus-rest-client-jackson
//DEPS io.smallrye:jandex:3.5.3
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN
//Q:CONFIG quarkus.index-dependency.core.group-id=io.debezium.jbang
//Q:CONFIG quarkus.index-dependency.core.artifact-id=debezium-jbang-core

package main;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Main to run DebeziumJBang
 */
@QuarkusMain
public class DebeziumJBang {

    public static void main(String... args) {
        Quarkus.run(DebeziumJBangMain.class, args);
    }

}
