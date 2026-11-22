/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationGrantError;
import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationRequest;
import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationResponse;
import com.nimbusds.oauth2.sdk.device.DeviceCode;
import com.nimbusds.oauth2.sdk.device.DeviceCodeGrant;
import com.nimbusds.oauth2.sdk.id.ClientID;

import net.minidev.json.JSONObject;

/**
 * The OAuth 2.0 device authorization grant (RFC 8628).
 * <p>
 * The SDK owns the request and response formats; the polling policy — when to stop, how long to
 * wait, and what to tell the user meanwhile — stays here, because that is the part RFC 8628 leaves
 * to the client.
 */
public final class DeviceFlow {

    public static final String GRANT_TYPE = DeviceCodeGrant.GRANT_TYPE.getValue();

    /** RFC 8628 §3.5: {@code slow_down} means add five seconds, and the increase is cumulative. */
    private static final int SLOW_DOWN_INCREMENT = 5;

    /** How often to reassure the user that a silent poll loop is still alive. */
    private static final Duration PROGRESS_EVERY = Duration.ofSeconds(30);

    private DeviceFlow() {
    }

    public static DeviceAuthorization authorize(OidcEndpoints endpoints, String clientId, String scope, Pkce pkce) {
        String url = endpoints.deviceAuthorizationEndpoint();

        // PKCE is not part of RFC 8628, so the SDK has no typed slot for it on this request; Keycloak
        // nonetheless binds the challenge here to the verifier sent at token exchange.
        DeviceAuthorizationRequest request = new DeviceAuthorizationRequest.Builder(new ClientID(clientId))
                .endpointURI(URI.create(url))
                .scope(Scope.parse(scope))
                .customParameter("code_challenge", pkce.challenge())
                .customParameter("code_challenge_method", "S256")
                .build();

        JSONObject body = OidcHttp.body(OidcHttp.send(request.toHTTPRequest(), url), url);

        DeviceAuthorizationResponse response;
        try {
            // Parsed from the body rather than the status: an authorization server is free to report
            // a refusal with 200, and the error field is what actually decides.
            response = DeviceAuthorizationResponse.parse(body);
        }
        catch (ParseException e) {
            throw new AuthException("Could not read the device authorization response from " + url
                    + " — " + e.getMessage(), e);
        }

        if (!response.indicatesSuccess()) {
            ErrorObject error = response.toErrorResponse().getErrorObject();
            throw new AuthException("Device authorization refused: " + error.getCode() + OidcHttp.describe(error));
        }
        return DeviceAuthorization.from(response.toSuccessResponse());
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

        String url = endpoints.tokenEndpoint();
        TokenRequest request = new TokenRequest.Builder(
                URI.create(url),
                new ClientID(clientId),
                new DeviceCodeGrant(new DeviceCode(authorization.deviceCode())))
                .customParameter("code_verifier", pkce.verifier())
                .build();

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

            JSONObject body = OidcHttp.body(OidcHttp.send(request.toHTTPRequest(), url), url);
            TokenResponse response;
            try {
                response = TokenResponse.parse(body);
            }
            catch (ParseException e) {
                throw new AuthException("Could not read the token response from " + url + " — " + e.getMessage(), e);
            }

            if (response.indicatesSuccess()) {
                // Stop immediately — a device_code is single-use and a further poll would come back
                // as invalid_grant, which reads like a failure after an actual success.
                return TokenSet.from(response.toSuccessResponse());
            }

            ErrorObject error = response.toErrorResponse().getErrorObject();
            String code = error.getCode();

            if (DeviceAuthorizationGrantError.AUTHORIZATION_PENDING.getCode().equals(code)) {
                if (Duration.between(lastProgress, Instant.now()).compareTo(PROGRESS_EVERY) >= 0) {
                    long remaining = Duration.between(Instant.now(), deadline).toSeconds();
                    progress.accept("Still waiting for approval… (" + remaining + "s left)");
                    lastProgress = Instant.now();
                }
            }
            else if (DeviceAuthorizationGrantError.SLOW_DOWN.getCode().equals(code)) {
                intervalSeconds += SLOW_DOWN_INCREMENT;
                progress.accept("Server asked us to slow down; polling every " + intervalSeconds + "s.");
            }
            else if (OAuth2Error.ACCESS_DENIED_CODE.equals(code)) {
                throw new AuthException("Login was denied in the browser.");
            }
            else if (DeviceAuthorizationGrantError.EXPIRED_TOKEN.getCode().equals(code)) {
                throw new AuthException("The login code expired before it was approved. Run 'debezium auth login' again.");
            }
            else {
                throw new AuthException("Login failed: " + code + OidcHttp.describe(error));
            }
        }
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
