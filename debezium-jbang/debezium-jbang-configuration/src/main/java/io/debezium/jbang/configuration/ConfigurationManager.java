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
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigurationManager {

    private static final String CONFIG_DIR = ".dbz";
    private static final String CONFIG_FILE = "config.yaml";
    private static final String DEFAULTS_RESOURCE = "config-defaults.yaml";
    private static final String CONFIGURATION_NAME = "global configuration";
    private static final Set<PosixFilePermission> DIR_MODE = PosixFilePermissions.fromString("rwx------");

    private final ObjectMapper objectMapper;
    private final Consumer<String> printer;

    // load() runs for every configuration read, so the warning would otherwise repeat.
    private boolean credentialsDirectoryWarned;

    public ConfigurationManager(ObjectMapper objectMapper,
                                Consumer<String> printer) {
        this.objectMapper = objectMapper;
        this.printer = printer;
    }

    public void save(Configuration configuration) {
        Path configDir = getConfigDir();
        Path configFile = configDir.resolve(CONFIG_FILE);

        try {
            Files.createDirectories(configDir);
            this.objectMapper.writeValue(configFile.toFile(), configuration);
        }
        catch (IOException e) {
            String message = "Failed to save " + CONFIGURATION_NAME + ": " + e.getMessage();
            this.printer.accept(e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    public Configuration load() {
        try {
            Configuration configuration = get();
            Path credentialsDirectory = configuration.getResolvedCredentials().getParent();
            if (credentialsDirectory != null && !Files.exists(credentialsDirectory)) {
                try {
                    Files.createDirectories(credentialsDirectory);
                }
                catch (IOException e) {
                    if (!credentialsDirectoryWarned) {
                        credentialsDirectoryWarned = true;
                        this.printer.accept("Warning: cannot create credentials directory " + credentialsDirectory + ": " + e.getMessage());
                    }
                    return configuration;
                }
                trySetPermissions(credentialsDirectory);
            }
            return configuration;
        }
        catch (Exception e) {
            this.printer.accept(e.getMessage());
            throw e;
        }
    }

    private static void trySetPermissions(Path directory) {
        try {
            Files.setPosixFilePermissions(directory, DIR_MODE);
        }
        catch (IOException | UnsupportedOperationException e) {
            // POSIX permissions do not exist on every filesystem.
        }
    }

    private Configuration get() {
        Path configFile = getConfigFilePath();

        if (!Files.exists(configFile)) {
            Configuration config = loadDefaults();
            save(config);

            return config;
        }

        Configuration defaults = loadDefaults();

        if (Files.exists(configFile)) {
            try {
                return this.objectMapper.readerForUpdating(defaults).readValue(configFile.toFile());
            }
            catch (IOException exception) {
                throw new RuntimeException("impossible to read the " + CONFIGURATION_NAME + " : " + exception.getMessage(), exception);
            }
        }

        return defaults;
    }

    private Configuration loadDefaults() {
        try (InputStream is = ConfigurationManager.class.getClassLoader().getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException(CONFIGURATION_NAME + " resource not found: " + DEFAULTS_RESOURCE);
            }
            return this.objectMapper.readValue(is, Configuration.class);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load " + CONFIGURATION_NAME + ": " + e.getMessage(), e);
        }
    }

    private Path getConfigDir() {
        return getHomeDir().resolve(CONFIG_DIR);
    }

    private Path getConfigFilePath() {
        return getConfigDir().resolve(CONFIG_FILE);
    }

    private Path getHomeDir() {
        String dir = System.getProperty("user.home");
        if (dir == null || dir.isBlank() || dir.startsWith("?")) {
            dir = System.getenv("HOME");
        }
        return Paths.get(dir);
    }
}
