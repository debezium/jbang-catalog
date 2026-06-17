/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.util;

import io.debezium.jbang.core.configuration.Configuration;
import io.debezium.jbang.core.configuration.Platform;

public class ConfigUtil {

    public static final String DEFAULT_PLATFORM_URL = "http://localhost:8080";

    private ConfigUtil() {
    }

    public static String getPlatformUrl() {
        Configuration config = Configuration.load();
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
