/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A token endpoint success response.
 * <p>
 * {@code expiresAt} is computed once on receipt rather than stored as a duration, so that a token
 * cached in memory can be checked for expiry later without tracking when it arrived.
 */
public record TokenSet(String accessToken, String refreshToken, String scope, Instant expiresAt) {

    /** Refresh this far before actual expiry, so a request in flight cannot cross the boundary. */
    private static final long LEEWAY_SECONDS = 30;

    static TokenSet from(JsonNode response) {
        String accessToken = OidcHttp.text(response, "access_token");
        if (accessToken == null) {
            throw new AuthException("Token response contained no access_token");
        }
        JsonNode expiresIn = response.get("expires_in");
        Instant expiry = expiresIn == null || expiresIn.isNull()
                ? null
                : Instant.now().plusSeconds(expiresIn.asLong());

        return new TokenSet(
                accessToken,
                OidcHttp.text(response, "refresh_token"),
                OidcHttp.text(response, "scope"),
                expiry);
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().plusSeconds(LEEWAY_SECONDS).isAfter(expiresAt);
    }
}
