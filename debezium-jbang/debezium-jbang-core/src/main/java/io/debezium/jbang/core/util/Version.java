/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import io.debezium.jbang.core.common.CommandLineHelper;

public class Version {
    public static final String POM_PATH = "/META-INF/maven/io.debezium/debezium-core/pom.properties";
    public static final String VERSION_PROPERTY = "version";
    public static final String JBANG_VERSION_FILE = "version.txt";
    public static final String JBANG_HOME = "JBANG_HOME";
    public static final String JBANG_CACHE = ".jbang/cache/";
    private final String core;
    private final String jbang;

    private static Version INSTANCE;

    private Version(String core, String jbang) {
        this.core = core;
        this.jbang = jbang;
    }

    public String jbang() {
        return jbang;
    }

    public String getCore() {
        return core;
    }

    public static synchronized Version getVersion() {
        if (INSTANCE == null) {
            INSTANCE = new Version(getCoreVersion(), getJBangVersion());
            return INSTANCE;
        }

        return INSTANCE;
    }

    private static String getJBangVersion() {
        try {
            if (System.getenv(JBANG_HOME) != null && !System.getenv(JBANG_HOME).isBlank()) {
                return Files.readString(Paths.get(System.getenv(JBANG_HOME)).resolve(JBANG_VERSION_FILE)).trim();
            }
            // fallback to .jbang cache that has a list of latest version
            return Files
                    .readString(Paths.get(CommandLineHelper.getHomeDir().toString())
                            .resolve(JBANG_CACHE + JBANG_VERSION_FILE));

        }
        catch (Exception ignore) {
            return null;
        }
    }

    public static String getCoreVersion() {
        try (InputStream is = Version.class.getResourceAsStream(POM_PATH)) {
            if (is != null) {
                Properties properties = new Properties();
                properties.load(is);
                return properties.getProperty(VERSION_PROPERTY, "");
            }
        }
        catch (Exception ignore) {
        }

        if (Version.class.getPackage() != null) {
            return Version.class.getPackage().getImplementationVersion();
        }

        return null;
    }
}
