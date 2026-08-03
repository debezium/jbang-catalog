/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

/**
 * Reads claims out of a JWT payload without verifying the signature.
 * <p>
 * That is correct here and not a shortcut: the CLI is not a resource server. The conductor validates
 * signature, issuer and expiry. The CLI only needs the claims for display ({@code auth status}) and
 * to know when to refresh proactively.
 */
public final class Jwt {

    private final JWTClaimsSet claims;

    private Jwt(JWTClaimsSet claims) {
        this.claims = claims;
    }

    public static Jwt parse(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthException("Cannot read an empty token");
        }
        try {
            return new Jwt(JWTParser.parse(token).getJWTClaimsSet());
        }
        catch (ParseException e) {
            throw new AuthException("Token is not a JWT: " + e.getMessage(), e);
        }
    }

    /** Null-safe: returns null rather than throwing, since every claim here is optional. */
    public String claim(String name) {
        try {
            return claims.getStringClaim(name);
        }
        catch (ParseException e) {
            // Present but not a string. For the identity and type claims read here that is the same
            // situation as absent, and not worth failing a status display over.
            return null;
        }
    }

    public Instant expiresAt() {
        Date expiry = claims.getExpirationTime();
        return expiry == null ? null : expiry.toInstant();
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
        return username != null ? username : claims.getSubject();
    }
}
