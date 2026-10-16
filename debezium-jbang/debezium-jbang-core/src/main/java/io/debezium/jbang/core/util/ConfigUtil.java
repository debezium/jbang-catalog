/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.util;

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.ConfigurationLoader;
import io.debezium.jbang.configuration.Environment;
import io.debezium.jbang.configuration.Platform;

public class ConfigUtil {

    public static final String DEFAULT_PLATFORM_URL = "http://localhost:8080";

    private ConfigUtil() {
    }

    public static String getPlatformUrl() {
        String envUrl = System.getenv("DEBEZIUM_PLATFORM_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl;
        }

        Configuration config = ConfigurationLoader.load();
        Platform platform = config.getPlatform();

        if (platform != null && platform.hasEnvironments()) {
            Environment env = platform.findActiveEnvironment();
            if (env != null && env.getUrl() != null && !env.getUrl().isBlank()) {
                return env.getUrl();
            }
        }

        return DEFAULT_PLATFORM_URL;
    }
}
