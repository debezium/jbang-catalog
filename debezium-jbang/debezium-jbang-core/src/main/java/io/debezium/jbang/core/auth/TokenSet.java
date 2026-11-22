/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.time.Instant;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;

/**
 * A token endpoint success response.
 * <p>
 * {@code expiresAt} is computed once on receipt rather than stored as a duration, so that a token
 * cached in memory can be checked for expiry later without tracking when it arrived. That is the
 * only reason this exists rather than passing the SDK's response around.
 */
public record TokenSet(String accessToken, String refreshToken, String scope, Instant expiresAt) {

    /** Refresh this far before actual expiry, so a request in flight cannot cross the boundary. */
    private static final long LEEWAY_SECONDS = 30;

    static TokenSet from(AccessTokenResponse response) {
        Tokens tokens = response.getTokens();
        AccessToken accessToken = tokens.getAccessToken();
        if (accessToken == null) {
            throw new AuthException("Token response contained no access_token");
        }
        RefreshToken refreshToken = tokens.getRefreshToken();
        Scope scope = accessToken.getScope();

        // The SDK reports an absent expires_in as a zero lifetime, which is not the same as a token
        // that expires now — it means the server did not say, so we must not treat it as stale.
        long lifetime = accessToken.getLifetime();

        return new TokenSet(
                accessToken.getValue(),
                refreshToken == null ? null : refreshToken.getValue(),
                scope == null ? null : scope.toString(),
                lifetime > 0 ? Instant.now().plusSeconds(lifetime) : null);
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().plusSeconds(LEEWAY_SECONDS).isAfter(expiresAt);
    }
}
