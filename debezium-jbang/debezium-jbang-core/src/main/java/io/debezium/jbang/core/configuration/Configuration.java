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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.debezium.jbang.core.util.CommandLineUtil;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private static final String CONFIG_DIR = ".dbz";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.yaml";

    private Map<String, Environment> environments;

    @JsonProperty("default")
    private String defaultEnv;

    private List<Platform> platforms;

    public Map<String, Environment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Map<String, Environment> environments) {
        this.environments = environments;
    }

    @JsonProperty("default")
    public String getDefaultEnv() {
        return defaultEnv;
    }

    @JsonProperty("default")
    public void setDefaultEnv(String defaultEnv) {
        this.defaultEnv = defaultEnv;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
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

    public void save() {
        Path configDir = CommandLineUtil.getHomeDir().resolve(CONFIG_DIR);
        Path configFile = configDir.resolve("config.yaml");
        try {
            Files.createDirectories(configDir);
            new ObjectMapper(new YAMLFactory()).writeValue(configFile.toFile(), this);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + e.getMessage(), e);
        }
    }
}
