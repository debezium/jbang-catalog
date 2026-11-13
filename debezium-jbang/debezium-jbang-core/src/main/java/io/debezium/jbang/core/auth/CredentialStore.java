/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.core.util.CommandLineUtil;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Refresh tokens on disk, at {@code ~/.dbz/credentials.json}, mode 0600.
 * <p>
 * Deliberately not {@code config.yaml}: that file holds platform addresses, looks shareable, and
 * eventually someone commits it. Credentials live in a separate file that never looks safe to copy.
 * <p>
 * Entries are keyed by <em>platform address</em>, matching the {@code --platform-address} resolution
 * chain, so one CLI can hold a local and a remote session at once and choosing the address chooses
 * the token.
 */
public final class CredentialStore {

    private static final String CONFIG_DIR = ".dbz";
    private static final String CREDENTIALS_FILE = "credentials.json";

    private static final Set<PosixFilePermission> FILE_MODE = PosixFilePermissions.fromString("rw-------");
    private static final Set<PosixFilePermission> DIR_MODE = PosixFilePermissions.fromString("rwx------");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CredentialStore() {
    }

    public static Path path() {
        return CommandLineUtil.getHomeDir().resolve(CONFIG_DIR).resolve(CREDENTIALS_FILE);
    }

    /** Trailing slashes would otherwise produce two entries for one platform. */
    public static String normalizeKey(String platformAddress) {
        if (platformAddress == null) {
            return null;
        }
        String trimmed = platformAddress.strip();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static StoredCredential find(String platformAddress) {
        return loadAll().get(normalizeKey(platformAddress));
    }

    public static Map<String, StoredCredential> loadAll() {
        Path file = path();
        try {
            // An empty file is what a write interrupted right after creating it leaves behind.
            if (!Files.exists(file) || Files.size(file) == 0) {
                return new LinkedHashMap<>();
            }
            return MAPPER.readValue(file.toFile(), CredentialsFile.class).credentials();
        }
        catch (IOException e) {
            throw new AuthException("Cannot read " + file + ": " + e.getMessage(), e);
        }
    }

    public static void save(String platformAddress, StoredCredential credential) {
        Map<String, StoredCredential> all = loadAll();
        all.put(normalizeKey(platformAddress), credential);
        writeAll(all);
    }

    /** @return true if there was something to remove. */
    public static boolean remove(String platformAddress) {
        Map<String, StoredCredential> all = loadAll();
        if (all.remove(normalizeKey(platformAddress)) == null) {
            return false;
        }
        writeAll(all);
        return true;
    }

    private static void writeAll(Map<String, StoredCredential> credentials) {
        Path file = path();
        try {
            Path directory = file.getParent();
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                trySetPermissions(directory, DIR_MODE);
            }
            // Create the file before writing so the mode is narrowed while it is still empty —
            // creating it with the default umask and chmod-ing afterwards leaves a window where the
            // token is world-readable.
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
            trySetPermissions(file, FILE_MODE);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), new CredentialsFile(credentials));
            trySetPermissions(file, FILE_MODE);
        }
        catch (IOException e) {
            throw new AuthException("Cannot write " + file + ": " + e.getMessage(), e);
        }
    }

    /** POSIX permissions do not exist on every filesystem; absence is not a failure worth aborting on. */
    private static void trySetPermissions(Path target, Set<PosixFilePermission> mode) {
        try {
            Files.setPosixFilePermissions(target, mode);
        }
        catch (IOException | UnsupportedOperationException e) {
            // Windows and some network mounts: nothing to do.
        }
    }

    /** The file as a whole: credentials keyed by platform address. */
    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CredentialsFile(@JsonProperty("credentials") Map<String, StoredCredential> credentials) {

        CredentialsFile {
            if (credentials == null) {
                credentials = new LinkedHashMap<>();
            }
        }
    }
}
