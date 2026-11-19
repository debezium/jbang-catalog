/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Unsigned JWTs for tests.
 * <p>
 * The signature is a placeholder because nothing under test verifies one — the CLI reads claims for
 * display and expiry only, and the conductor is what actually validates tokens.
 */
final class JwtFixtures {

    static final String ACCESS_TOKEN = accessToken("someone@example.com", Instant.now().plusSeconds(300));

    private JwtFixtures() {
    }

    static String accessToken(String email, Instant expiry) {
        return token("{\"sub\":\"3f9a-user\",\"email\":\"" + email + "\",\"preferred_username\":\"someone\","
                + "\"typ\":\"Bearer\",\"exp\":" + expiry.getEpochSecond() + "}");
    }

    static String refreshToken(String type) {
        return token("{\"sub\":\"3f9a-user\",\"typ\":\"" + type + "\"}");
    }

    private static String token(String payload) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + ".not-a-real-signature";
    }
}
