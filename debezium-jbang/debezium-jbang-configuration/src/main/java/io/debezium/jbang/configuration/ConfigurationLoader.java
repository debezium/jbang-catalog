/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public class ConfigurationLoader {

    private static final String CONFIG_DIR = ".dbz";
    private static final String CONFIG_FILE = "config.yaml";
    private static final String DEFAULTS_RESOURCE = "config-defaults.yaml";

    private static final ObjectMapper MAPPER = new ObjectMapper(
            new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));

    private ConfigurationLoader() {
    }

    public static Configuration loadDefaults() {
        try (InputStream is = ConfigurationLoader.class.getClassLoader().getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Default configuration resource not found: " + DEFAULTS_RESOURCE);
            }
            return MAPPER.readValue(is, Configuration.class);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load default configuration: " + e.getMessage(), e);
        }
    }

    public static Configuration load() {
        return load(getConfigFilePath());
    }

    static Configuration load(Path configFile) {
        Configuration defaults = loadDefaults();

        if (Files.exists(configFile)) {
            try {
                return MAPPER.readerForUpdating(defaults).readValue(configFile.toFile());
            }
            catch (IOException ignore) {
            }
        }

        return defaults;
    }

    public static void save(Configuration config) {
        save(config, getConfigDir());
    }

    static void save(Configuration config, Path configDir) {
        Path configFile = configDir.resolve(CONFIG_FILE);
        try {
            Files.createDirectories(configDir);
            MAPPER.writeValue(configFile.toFile(), config);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + e.getMessage(), e);
        }
    }

    public static void initializeIfAbsent() {
        Path configFile = getConfigFilePath();
        if (!Files.exists(configFile)) {
            save(loadDefaults());
        }
    }

    private static Path getConfigDir() {
        return getHomeDir().resolve(CONFIG_DIR);
    }

    private static Path getConfigFilePath() {
        return getConfigDir().resolve(CONFIG_FILE);
    }

    private static Path getHomeDir() {
        String dir = System.getProperty("user.home");
        if (dir == null || dir.isBlank() || dir.startsWith("?")) {
            dir = System.getenv("HOME");
        }
        return Paths.get(dir);
    }
}
