/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.net.URI;

import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationSuccessResponse;

/**
 * The device authorization response (RFC 8628 §3.2).
 * <p>
 * The {@code deviceCode} is a server-minted secret known only to this process and is never
 * displayed; the short {@code userCode} is the only thing the human sees. They are unrelated
 * mechanisms — the device grant has no authorization code, which is why PKCE protects nothing here
 * on its own and is carried only because Keycloak binds it to the token exchange.
 * <p>
 * Kept as a record of plain strings rather than exposing the SDK response, so that the commands and
 * their tests do not have to know which library parsed the wire format.
 */
public record DeviceAuthorization(String deviceCode,
        String userCode,
        String verificationUri,
        String verificationUriComplete,
        int intervalSeconds,
        int expiresInSeconds) {

    /** The SDK applies RFC 8628 §3.2's default of 5 seconds when {@code interval} is absent. */
    static DeviceAuthorization from(DeviceAuthorizationSuccessResponse response) {
        URI complete = response.getVerificationURIComplete();

        return new DeviceAuthorization(
                response.getDeviceCode().getValue(),
                response.getUserCode().getValue(),
                response.getVerificationURI().toString(),
                complete == null ? null : complete.toString(),
                (int) response.getInterval(),
                (int) response.getLifetime());
    }
}
