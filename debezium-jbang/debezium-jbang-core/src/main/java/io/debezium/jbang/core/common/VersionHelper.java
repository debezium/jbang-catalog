/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class VersionHelper {

    private static volatile String coreVersion;
    private static volatile String jbangVersion;

    private VersionHelper() {
    }

    public static String getJBangVersion() {
        if (jbangVersion != null) {
            return jbangVersion;
        }

        try {
            // find actual version in JBANG_HOME
            String homeDir = System.getenv("JBANG_HOME");
            String path = "";
            if (homeDir == null || homeDir.isBlank()) {
                // fallback to .jbang cache that has a list of latest version
                path = ".jbang/cache/";
                homeDir = CommandLineHelper.getHomeDir().toString();
            }
            Path file = Paths.get(homeDir).resolve(path + "version.txt");
            if (Files.exists(file) && Files.isRegularFile(file)) {
                jbangVersion = Files.readString(file).trim();

                return jbangVersion;
            }
        }
        catch (Exception e) {
            // ignore
        }
        return null;
    }

    public static synchronized String getCoreVersion() {
        if (coreVersion != null) {
            return coreVersion;
        }
        // First, try to load from maven properties
        InputStream is = null;
        try {
            Properties p = new Properties();
            is = VersionHelper.class.getResourceAsStream("/META-INF/maven/io.debezium/debezium-core/pom.properties");
            if (is != null) {
                p.load(is);
                coreVersion = p.getProperty("version", "");
            }
        }
        catch (Exception e) {
            // ignore
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (Exception e) {
                    // ignore
                }
            }
        }

        // Fallback to using Java API
        if (coreVersion == null) {
            Package aPackage = VersionHelper.class.getPackage();
            if (aPackage != null) {
                coreVersion = aPackage.getImplementationVersion();
                if (coreVersion == null) {
                    coreVersion = aPackage.getSpecificationVersion();
                }
            }
        }

        if (coreVersion == null) {
            // we could not compute the version so use a blank
            coreVersion = "";
        }

        return coreVersion;
    }
}
