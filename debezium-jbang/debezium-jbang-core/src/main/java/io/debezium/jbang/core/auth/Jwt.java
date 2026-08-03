/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads claims out of a JWT payload without verifying the signature.
 * <p>
 * That is correct here and not a shortcut: the CLI is not a resource server. The conductor validates
 * signature, issuer and expiry. The CLI only needs the claims for display ({@code auth status}) and
 * to know when to refresh proactively.
 */
public final class Jwt {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode claims;

    private Jwt(JsonNode claims) {
        this.claims = claims;
    }

    public static Jwt parse(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthException("Cannot read an empty token");
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new AuthException("Token is not a JWT (expected three dot-separated parts)");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return new Jwt(MAPPER.readTree(new String(payload, StandardCharsets.UTF_8)));
        }
        catch (Exception e) {
            throw new AuthException("Cannot decode token payload: " + e.getMessage(), e);
        }
    }

    /** Null-safe: returns null rather than throwing, since every claim here is optional. */
    public String claim(String name) {
        JsonNode value = claims.get(name);
        return value == null || value.isNull() ? null : value.asText();
    }

    public Instant expiresAt() {
        JsonNode exp = claims.get("exp");
        return exp == null || exp.isNull() ? null : Instant.ofEpochSecond(exp.asLong());
    }

    /**
     * The {@code typ} claim, which on a Keycloak refresh token distinguishes {@code Offline} from
     * {@code Refresh}. Only an {@code Offline} token survives the realm's SSO idle timeout, so this
     * is what proves {@code offline_access} was actually granted rather than merely requested.
     */
    public String type() {
        return claim("typ");
    }

    /** Best available human identity, in descending order of usefulness. */
    public String subjectLabel() {
        String email = claim("email");
        if (email != null) {
            return email;
        }
        String username = claim("preferred_username");
        return username != null ? username : claim("sub");
    }
}
