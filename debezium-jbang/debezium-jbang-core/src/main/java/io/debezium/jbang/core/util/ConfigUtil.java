/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.util;

import java.util.Map;

import io.debezium.jbang.core.configuration.Configuration;
import io.debezium.jbang.core.configuration.Environment;
import io.debezium.jbang.core.configuration.Platform;

public class ConfigUtil {

    public static final String DEFAULT_PLATFORM_URL = "http://localhost:8080";

    private ConfigUtil() {
    }

    public static String getPlatformUrl() {
        String envUrl = System.getenv("DEBEZIUM_PLATFORM_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl;
        }

        Configuration config = Configuration.load();

        // New schema: environments map + top-level default key
        Map<String, Environment> envs = config.getEnvironments();
        if (envs != null && !envs.isEmpty()) {
            String active = config.getDefaultEnv();
            if (active != null && envs.containsKey(active)) {
                String url = envs.get(active).getUrl();
                if (url != null && !url.isBlank()) {
                    return url;
                }
            }
            // No default set — fall back to first entry
            Environment first = envs.values().iterator().next();
            if (first.getUrl() != null && !first.getUrl().isBlank()) {
                return first.getUrl();
            }
        }

        // Legacy schema: platforms list (backward compat)
        if (config.getPlatforms() != null) {
            return config.getPlatforms().stream()
                    .filter(Platform::isDefault)
                    .findFirst()
                    .map(Platform::getAddress)
                    .orElse(DEFAULT_PLATFORM_URL);
        }

        return DEFAULT_PLATFORM_URL;
    }
}
