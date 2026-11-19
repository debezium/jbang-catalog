/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.auth;

import java.time.Instant;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.auth.AuthException;
import io.debezium.jbang.core.auth.CredentialStore;
import io.debezium.jbang.core.auth.DeviceAuthorization;
import io.debezium.jbang.core.auth.DeviceFlow;
import io.debezium.jbang.core.auth.Jwt;
import io.debezium.jbang.core.auth.OidcDiscovery;
import io.debezium.jbang.core.auth.OidcEndpoints;
import io.debezium.jbang.core.auth.Pkce;
import io.debezium.jbang.core.auth.StoredCredential;
import io.debezium.jbang.core.auth.TokenProvider;
import io.debezium.jbang.core.auth.TokenSet;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;

import picocli.CommandLine;

@CommandLine.Command(name = "login", description = "Log in to a Debezium Platform and store the session", mixinStandardHelpOptions = true, sortOptions = false)
public class AuthLogin extends DebeziumCommand {

    /**
     * {@code offline_access} is the one that matters: without it Keycloak issues a session-bound
     * refresh token that dies on the realm's SSO idle timeout, typically 30 minutes.
     */
    private static final String DEFAULT_SCOPE = "openid profile email offline_access";

    private static final String DEFAULT_CLIENT_ID = "dbz-cli";

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    @CommandLine.Option(names = { "--issuer" }, description = "OIDC issuer URL (overrides DEBEZIUM_OIDC_ISSUER and ~/.dbz/config.yaml)")
    String issuer;

    @CommandLine.Option(names = { "--client-id" }, description = "OAuth client id (default: " + DEFAULT_CLIENT_ID + ")")
    String clientId;

    @CommandLine.Option(names = { "--scope" }, description = "OAuth scopes to request (default: \"" + DEFAULT_SCOPE + "\")")
    String scope;

    @CommandLine.Option(names = {
            "--device" }, description = "Use the device authorization grant. Currently the only supported flow, so this is the default.")
    boolean device;

    public AuthLogin(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        String platformAddress = CredentialStore.normalizeKey(platformFactory.resolvedApiUrl(configuration()).toString());
        String resolvedIssuer = resolveIssuer(platformAddress);
        String resolvedClientId = resolveClientId();
        String resolvedScope = scope != null && !scope.isBlank() ? scope : DEFAULT_SCOPE;

        println("Platform: " + platformAddress);
        println("Issuer:   " + resolvedIssuer);
        println("Client:   " + resolvedClientId);
        println();

        OidcEndpoints endpoints = OidcDiscovery.discover(resolvedIssuer);

        Pkce pkce = Pkce.generate();
        DeviceAuthorization authorization = DeviceFlow.authorize(endpoints, resolvedClientId, resolvedScope, pkce);

        printInstructions(authorization);

        TokenSet tokens = DeviceFlow.poll(endpoints, resolvedClientId, authorization, pkce, this::println);

        Jwt accessToken = Jwt.parse(tokens.accessToken());
        String refreshTokenType = describeRefreshToken(tokens);

        CredentialStore.save(platformAddress, new StoredCredential(
                endpoints.issuer(),
                resolvedClientId,
                tokens.refreshToken(),
                accessToken.subjectLabel(),
                refreshTokenType,
                Instant.now().toString()));

        TokenProvider.cache(platformAddress, tokens);

        println();
        println("Logged in as " + accessToken.subjectLabel() + ".");
        println("Session saved to " + CredentialStore.path() + " (mode 0600).");
        warnIfNotOffline(refreshTokenType);
        return 0;
    }

    private void printInstructions(DeviceAuthorization authorization) {
        println("To authorize, open:");
        println("  " + authorization.verificationUri());
        println("and enter the code:");
        println("  " + authorization.userCode());
        if (authorization.verificationUriComplete() != null) {
            // The complete URL is the convenient path; the bare pair above is the one that works
            // when the terminal and the browser are on different machines.
            println();
            println("Or open this directly:");
            println("  " + authorization.verificationUriComplete());
        }
        println();
        println("Waiting for approval (expires in " + authorization.expiresInSeconds() + "s)…");
    }

    /**
     * Reads {@code typ} off the refresh token. {@code Offline} is what makes the session survive the
     * SSO idle timeout — the accepted scope alone does not prove it was granted.
     */
    private String describeRefreshToken(TokenSet tokens) {
        if (tokens.refreshToken() == null) {
            return null;
        }
        try {
            return Jwt.parse(tokens.refreshToken()).type().orElse(null);
        }
        catch (AuthException e) {
            // Opaque refresh tokens are legitimate on other providers; nothing to report.
            return null;
        }
    }

    private void warnIfNotOffline(String refreshTokenType) {
        if (refreshTokenType != null && !"Offline".equalsIgnoreCase(refreshTokenType)) {
            println();
            println("Note: this is a '" + refreshTokenType + "' token, not an offline one — it will expire with the");
            println("      SSO session (often 30 minutes idle). Check that the 'offline_access' scope is assigned");
            println("      to this client and that the user holds the offline_access realm role.");
        }
    }

    private String resolveIssuer(String platformAddress) {
        if (issuer != null && !issuer.isBlank()) {
            return issuer;
        }
        String configured = configuration().getResolvedIssuer();
        if (configured != null) {
            return configured;
        }
        // Logging in again against a platform we already know: reuse what worked last time.
        StoredCredential existing = CredentialStore.find(platformAddress);
        if (existing != null && existing.issuer() != null) {
            return existing.issuer();
        }
        throw new AuthException("No OIDC issuer configured for " + platformAddress + ". Pass --issuer <url>, "
                + "set DEBEZIUM_OIDC_ISSUER, or add 'issuer' to the environment in ~/.dbz/config.yaml");
    }

    private String resolveClientId() {
        if (clientId != null && !clientId.isBlank()) {
            return clientId;
        }
        String configured = configuration().getResolvedClientId();
        return configured != null ? configured : DEFAULT_CLIENT_ID;
    }
}
