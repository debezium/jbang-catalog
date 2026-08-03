/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The OAuth 2.0 device authorization grant (RFC 8628).
 */
public final class DeviceFlow {

    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";

    /** RFC 8628 §3.5: {@code slow_down} means add five seconds, and the increase is cumulative. */
    private static final int SLOW_DOWN_INCREMENT = 5;

    /** How often to reassure the user that a silent poll loop is still alive. */
    private static final Duration PROGRESS_EVERY = Duration.ofSeconds(30);

    private DeviceFlow() {
    }

    public static DeviceAuthorization authorize(OidcEndpoints endpoints, String clientId, String scope, Pkce pkce) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        form.put("scope", scope);
        form.put("code_challenge", pkce.challenge());
        form.put("code_challenge_method", "S256");

        return DeviceAuthorization.from(OidcHttp.postForm(endpoints.deviceAuthorizationEndpoint(), form));
    }

    /**
     * Polls the token endpoint until the human approves, denies, or the device code expires.
     * <p>
     * Every non-success is an HTTP 400 carrying a JSON {@code error} field — {@code
     * authorization_pending} and {@code access_denied} are indistinguishable by status code. This
     * branches on {@code error} alone; branching on the status would turn a denial into a poll loop
     * that runs until timeout.
     */
    public static TokenSet poll(OidcEndpoints endpoints,
                                String clientId,
                                DeviceAuthorization authorization,
                                Pkce pkce,
                                Consumer<String> progress) {

        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        form.put("grant_type", GRANT_TYPE);
        form.put("device_code", authorization.deviceCode());
        form.put("code_verifier", pkce.verifier());

        // Bound the loop on wall-clock time, not iteration count: the interval grows on slow_down,
        // so a fixed number of attempts would not correspond to expires_in.
        Instant deadline = Instant.now().plusSeconds(authorization.expiresInSeconds());
        int intervalSeconds = authorization.intervalSeconds();
        Instant lastProgress = Instant.now();

        while (true) {
            if (Instant.now().isAfter(deadline)) {
                throw new AuthException("The login code expired before it was approved. Run 'debezium auth login' again.");
            }

            sleep(intervalSeconds);
            JsonNode response = OidcHttp.postForm(endpoints.tokenEndpoint(), form);
            String error = OidcHttp.text(response, "error");

            if (error == null) {
                // Success. Stop immediately — a device_code is single-use and a further poll would
                // come back as invalid_grant, which reads like a failure after an actual success.
                return TokenSet.from(response);
            }

            switch (error) {
                case "authorization_pending":
                    if (Duration.between(lastProgress, Instant.now()).compareTo(PROGRESS_EVERY) >= 0) {
                        long remaining = Duration.between(Instant.now(), deadline).toSeconds();
                        progress.accept("Still waiting for approval… (" + remaining + "s left)");
                        lastProgress = Instant.now();
                    }
                    break;

                case "slow_down":
                    intervalSeconds += SLOW_DOWN_INCREMENT;
                    progress.accept("Server asked us to slow down; polling every " + intervalSeconds + "s.");
                    break;

                case "access_denied":
                    throw new AuthException("Login was denied in the browser.");

                case "expired_token":
                    throw new AuthException("The login code expired before it was approved. Run 'debezium auth login' again.");

                default:
                    throw new AuthException("Login failed: " + error + description(response));
            }
        }
    }

    private static String description(JsonNode response) {
        String detail = OidcHttp.text(response, "error_description");
        return detail == null ? "" : " (" + detail + ")";
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthException("Login was interrupted.");
        }
    }
}
