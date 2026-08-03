/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the access token to send for a given platform address.
 * <p>
 * Two rules keep this from becoming a mess:
 * <ul>
 * <li><strong>The access token never touches disk.</strong> It lives in this process only and is
 * re-derived from the stored refresh token whenever it is missing or close to expiry.</li>
 * <li><strong>Refresh proactively, not reactively.</strong> The access token carries its own
 * expiry, so there is no need to send one we already know is stale and learn about it from a 401.
 * A 401 that survives a fresh token is a configuration problem — audience, issuer, or a revoked
 * session — and retrying it would only turn an error into a spin.</li>
 * </ul>
 */
public final class TokenProvider {

    /** Access tokens, keyed by normalized platform address. Process lifetime only. */
    private static final Map<String, TokenSet> ACCESS_TOKENS = new ConcurrentHashMap<>();

    /** Discovery documents, keyed by issuer, so a refresh costs one request rather than two. */
    private static final Map<String, OidcEndpoints> ENDPOINTS = new ConcurrentHashMap<>();

    private TokenProvider() {
    }

    public static boolean hasCredential(String platformAddress) {
        return CredentialStore.find(platformAddress) != null;
    }

    public static OidcEndpoints endpointsFor(String issuer) {
        return ENDPOINTS.computeIfAbsent(issuer, OidcDiscovery::discover);
    }

    /**
     * @return a valid access token, or {@code null} when no credential is stored for this address —
     *         which is not an error. An unauthenticated platform (a local conductor with
     *         {@code apiPolicy: permit}) must keep working exactly as it did before auth existed.
     */
    public static String accessTokenOrNull(String platformAddress) {
        String key = CredentialStore.normalizeKey(platformAddress);

        TokenSet cached = ACCESS_TOKENS.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.accessToken();
        }

        StoredCredential credential = CredentialStore.find(key);
        if (credential == null) {
            return null;
        }

        TokenSet refreshed = TokenEndpoint.refresh(
                endpointsFor(credential.issuer()),
                credential.clientId(),
                credential.refreshToken());

        persistRotatedRefreshToken(key, credential, refreshed);
        ACCESS_TOKENS.put(key, refreshed);
        return refreshed.accessToken();
    }

    /**
     * Keycloak may rotate the refresh token on every use. Dropping the new one leaves the stored
     * credential valid only until the old token's grace period lapses, which presents days later as
     * an unexplained logout.
     */
    private static void persistRotatedRefreshToken(String key, StoredCredential credential, TokenSet refreshed) {
        String rotated = refreshed.refreshToken();
        if (rotated != null && !rotated.equals(credential.refreshToken())) {
            CredentialStore.save(key, new StoredCredential(
                    credential.issuer(),
                    credential.clientId(),
                    rotated,
                    credential.subject(),
                    credential.refreshTokenType(),
                    credential.obtainedAt()));
        }
    }

    public static void cache(String platformAddress, TokenSet tokens) {
        ACCESS_TOKENS.put(CredentialStore.normalizeKey(platformAddress), tokens);
    }

    public static void invalidate(String platformAddress) {
        ACCESS_TOKENS.remove(CredentialStore.normalizeKey(platformAddress));
    }
}
