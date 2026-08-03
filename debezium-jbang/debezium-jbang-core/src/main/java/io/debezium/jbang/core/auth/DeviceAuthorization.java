/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The device authorization response (RFC 8628 §3.2).
 * <p>
 * The {@code deviceCode} is a server-minted secret known only to this process and is never
 * displayed; the short {@code userCode} is the only thing the human sees. They are unrelated
 * mechanisms — the device grant has no authorization code, which is why PKCE protects nothing here
 * on its own and is carried only because Keycloak binds it to the token exchange.
 */
public record DeviceAuthorization(String deviceCode,
        String userCode,
        String verificationUri,
        String verificationUriComplete,
        int intervalSeconds,
        int expiresInSeconds) {

    /** RFC 8628 §3.2: absent {@code interval} means 5 seconds. */
    private static final int DEFAULT_INTERVAL = 5;

    private static final int DEFAULT_EXPIRY = 600;

    static DeviceAuthorization from(JsonNode response) {
        String deviceCode = OidcHttp.text(response, "device_code");
        String userCode = OidcHttp.text(response, "user_code");
        String verificationUri = OidcHttp.text(response, "verification_uri");

        if (deviceCode == null || userCode == null || verificationUri == null) {
            String error = OidcHttp.text(response, "error");
            throw new AuthException(error != null
                    ? "Device authorization refused: " + error
                            + describe(OidcHttp.text(response, "error_description"))
                    : "Device authorization response was missing device_code, user_code or verification_uri");
        }

        JsonNode interval = response.get("interval");
        JsonNode expiresIn = response.get("expires_in");

        return new DeviceAuthorization(
                deviceCode,
                userCode,
                verificationUri,
                OidcHttp.text(response, "verification_uri_complete"),
                interval == null || interval.isNull() ? DEFAULT_INTERVAL : interval.asInt(),
                expiresIn == null || expiresIn.isNull() ? DEFAULT_EXPIRY : expiresIn.asInt());
    }

    private static String describe(String errorDescription) {
        return errorDescription == null ? "" : " (" + errorDescription + ")";
    }
}
