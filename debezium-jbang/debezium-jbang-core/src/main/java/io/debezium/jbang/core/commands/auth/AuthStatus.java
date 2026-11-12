/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.auth.AuthException;
import io.debezium.jbang.core.auth.CredentialStore;
import io.debezium.jbang.core.auth.Jwt;
import io.debezium.jbang.core.auth.StoredCredential;
import io.debezium.jbang.core.auth.TokenProvider;
import io.debezium.jbang.core.auth.TokenSet;
import io.debezium.jbang.core.commands.DebeziumCommand;

import picocli.CommandLine;

/**
 * Shows which platforms have stored sessions.
 * <p>
 * Worth having early: it is how the other two commands get debugged, and it is the thing that
 * proves a session actually persisted across terminals rather than living in one shell's memory.
 */
@CommandLine.Command(name = "status", description = "Show stored platform sessions", mixinStandardHelpOptions = true, sortOptions = false)
public class AuthStatus extends DebeziumCommand {

    @CommandLine.Option(names = {
            "--verify" }, description = "Contact the issuer to confirm each session is still live (makes network calls)")
    boolean verify;

    public AuthStatus(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Map<String, StoredCredential> credentials = CredentialStore.loadAll();

        if (credentials.isEmpty()) {
            println("Not logged in to any platform.");
            println("Run 'debezium auth login --issuer <url>' to sign in.");
            return 1;
        }

        println("Credentials file: " + CredentialStore.path());
        println();

        boolean allHealthy = true;
        for (Map.Entry<String, StoredCredential> entry : credentials.entrySet()) {
            allHealthy &= printCredential(entry.getKey(), entry.getValue());
        }
        return allHealthy ? 0 : 1;
    }

    private boolean printCredential(String platformAddress, StoredCredential credential) {
        println(platformAddress);
        println("  Identity:   " + orUnknown(credential.subject()));
        println("  Issuer:     " + orUnknown(credential.issuer()));
        println("  Client:     " + orUnknown(credential.clientId()));
        println("  Token type: " + describeTokenType(credential.refreshTokenType()));
        println("  Logged in:  " + orUnknown(credential.obtainedAt()));

        if (!verify) {
            println();
            return true;
        }

        boolean healthy = true;
        try {
            // Through TokenProvider rather than the token endpoint directly, so that a refresh token
            // rotated by this check is saved instead of being thrown away.
            TokenSet refreshed = TokenProvider.refresh(platformAddress, credential);
            Jwt accessToken = Jwt.parse(refreshed.accessToken());
            println("  Live:       yes — access token valid for " + remaining(refreshed) + ", as " + accessToken.subjectLabel());
        }
        catch (AuthException e) {
            println("  Live:       NO — " + e.getMessage());
            println("              Run 'debezium auth login --platform-address " + platformAddress + "'");
            healthy = false;
        }
        println();
        return healthy;
    }

    private String remaining(TokenSet tokens) {
        Instant expiry = tokens.expiresAt();
        if (expiry == null) {
            return "an unspecified period";
        }
        long seconds = Duration.between(Instant.now(), expiry).toSeconds();
        return seconds < 120 ? seconds + "s" : (seconds / 60) + "m";
    }

    /** Surfaces the offline-vs-session distinction, since it decides how long the login lasts. */
    private String describeTokenType(String refreshTokenType) {
        if (refreshTokenType == null) {
            return "unknown (opaque refresh token)";
        }
        return "Offline".equalsIgnoreCase(refreshTokenType)
                ? "Offline — survives SSO idle timeout"
                : refreshTokenType + " — expires with the SSO session";
    }

    private String orUnknown(String value) {
        return value == null || value.isBlank() ? "(unknown)" : value;
    }
}
