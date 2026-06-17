/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.debezium.jbang.core.util.CommandLineUtil;

public class Configuration {

    private static final String CONFIG_FILE = ".dbz/config.yaml";

    private List<Platform> platforms;

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public static Configuration load() {
        Path configFile = CommandLineUtil.getHomeDir().resolve(CONFIG_FILE);
        if (Files.exists(configFile)) {
            try {
                return new ObjectMapper(new YAMLFactory()).readValue(configFile.toFile(), Configuration.class);
            }
            catch (IOException ignore) {
            }
        }
        return new Configuration();
    }
}
