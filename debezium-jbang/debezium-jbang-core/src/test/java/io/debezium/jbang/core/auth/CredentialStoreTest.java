/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CredentialStoreTest {

    @TempDir
    Path home;

    private String originalHome;

    @BeforeEach
    public void redirectHome() {
        originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());
    }

    @AfterEach
    public void restoreHome() {
        if (originalHome != null) {
            System.setProperty("user.home", originalHome);
        }
    }

    private static StoredCredential credential(String refreshToken) {
        return new StoredCredential("https://auth.example.com/realms/dmp", "dbz-cli", refreshToken,
                "someone@example.com", "Offline", "2026-08-03T10:00:00Z");
    }

    @Test
    public void shouldRoundTripACredential() {
        CredentialStore.save("https://api.example.com", credential("refresh-1"));

        StoredCredential loaded = CredentialStore.find("https://api.example.com");

        assertThat(loaded).isNotNull();
        assertThat(loaded.refreshToken()).isEqualTo("refresh-1");
        assertThat(loaded.issuer()).isEqualTo("https://auth.example.com/realms/dmp");
        assertThat(loaded.subject()).isEqualTo("someone@example.com");
        assertThat(loaded.refreshTokenType()).isEqualTo("Offline");
    }

    /**
     * The file holds a long-lived credential, so the mode is the security boundary — the default
     * umask is not enough on a shared machine.
     */
    @Test
    public void shouldCreateTheFileMode0600() throws Exception {
        CredentialStore.save("https://api.example.com", credential("refresh-1"));

        Path file = CredentialStore.path();

        assertThat(Files.exists(file)).isTrue();
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(file))).isEqualTo("rw-------");
    }

    /** Otherwise one platform accumulates an entry per spelling of its address. */
    @Test
    public void shouldTreatTrailingSlashesAsTheSamePlatform() {
        CredentialStore.save("https://api.example.com/", credential("refresh-1"));

        assertThat(CredentialStore.find("https://api.example.com")).isNotNull();
        assertThat(CredentialStore.loadAll()).hasSize(1);
    }

    /** One CLI holds a local and a remote session at once; the address selects the token. */
    @Test
    public void shouldKeepCredentialsForSeveralPlatformsSideBySide() {
        CredentialStore.save("https://api.example.com", credential("remote-token"));
        CredentialStore.save("http://localhost:8080", credential("local-token"));

        assertThat(CredentialStore.find("https://api.example.com").refreshToken()).isEqualTo("remote-token");
        assertThat(CredentialStore.find("http://localhost:8080").refreshToken()).isEqualTo("local-token");
        assertThat(CredentialStore.loadAll()).hasSize(2);
    }

    @Test
    public void shouldOverwriteTheCredentialForAKnownPlatform() {
        CredentialStore.save("https://api.example.com", credential("old"));
        CredentialStore.save("https://api.example.com", credential("new"));

        assertThat(CredentialStore.find("https://api.example.com").refreshToken()).isEqualTo("new");
        assertThat(CredentialStore.loadAll()).hasSize(1);
    }

    @Test
    public void shouldRemoveOnlyTheRequestedPlatform() {
        CredentialStore.save("https://api.example.com", credential("remote-token"));
        CredentialStore.save("http://localhost:8080", credential("local-token"));

        assertThat(CredentialStore.remove("https://api.example.com")).isTrue();

        assertThat(CredentialStore.find("https://api.example.com")).isNull();
        assertThat(CredentialStore.find("http://localhost:8080")).isNotNull();
    }

    @Test
    public void shouldReportNothingRemovedForAnUnknownPlatform() {
        assertThat(CredentialStore.remove("https://api.unknown.example")).isFalse();
    }

    @Test
    public void shouldReturnNoCredentialsWhenTheFileDoesNotExist() {
        assertThat(CredentialStore.loadAll()).isEmpty();
        assertThat(CredentialStore.find("https://api.example.com")).isNull();
    }
}
