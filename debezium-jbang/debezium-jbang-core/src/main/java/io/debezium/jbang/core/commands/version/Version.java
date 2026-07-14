/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.version;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Version {
    public static final String POM_PATH = "/META-INF/maven/io.debezium.jbang/debezium-jbang-core/pom.properties";
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
            String jbangHome = System.getenv(JBANG_HOME);
            if (jbangHome != null && !jbangHome.isBlank()) {
                String version = Files.readString(Paths.get(jbangHome).resolve(JBANG_VERSION_FILE)).trim();
                if (!version.isBlank()) {
                    return version;
                }
            }
        }
        catch (Exception ignore) {
        }

        // Resolve jbang binary by well-known absolute path so this works even when jbang is not on PATH
        Path userHome = Paths.get(System.getProperty("user.home"));
        for (Path candidate : new Path[]{ userHome.resolve(".jbang/bin/jbang"), userHome.resolve(".sdkman/candidates/jbang/current/bin/jbang") }) {
            if (Files.exists(candidate)) {
                try {
                    Process p = new ProcessBuilder(candidate.toString(), "version").start();
                    String version = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0 && !version.isBlank()) {
                        return version;
                    }
                }
                catch (Exception ignore) {
                }
                break;
            }
        }

        return null;
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
