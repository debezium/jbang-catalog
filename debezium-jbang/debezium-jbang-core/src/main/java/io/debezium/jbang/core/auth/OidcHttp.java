/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.io.IOException;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import net.minidev.json.JSONObject;

/**
 * Shared plumbing for the OAuth endpoint calls: timeouts, transport errors, and reading a JSON body.
 * <p>
 * Deliberately not the Quarkus REST client: these calls go to the authorization server, not to the
 * platform API, so they must not pick up the bearer filter or the platform base URI.
 */
final class OidcHttp {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private OidcHttp() {
    }

    /** Applies the CLI's timeouts and turns a transport failure into a message naming the endpoint. */
    static HTTPResponse send(HTTPRequest request, String url) {
        request.setConnectTimeout(CONNECT_TIMEOUT_MS);
        request.setReadTimeout(READ_TIMEOUT_MS);
        try {
            return request.send();
        }
        catch (IOException e) {
            throw new AuthException("Cannot reach " + url + " — " + e.getMessage(), e);
        }
    }

    /**
     * Reads the JSON body whatever the status was.
     * <p>
     * The OAuth error taxonomy lives in the JSON {@code error} field, not in the status code —
     * {@code authorization_pending} and {@code access_denied} are both HTTP 400 — so the caller has
     * to see the body of a failed response, not an exception.
     */
    static JSONObject body(HTTPResponse response, String url) {
        try {
            return response.getBodyAsJSONObject();
        }
        catch (ParseException e) {
            throw new AuthException("Non-JSON response (HTTP " + response.getStatusCode() + ") from " + url
                    + " — is this an OIDC issuer URL? First bytes: " + head(response.getBody()));
        }
    }

    /** Renders {@code error_description} as a trailing parenthetical, or nothing when absent. */
    static String describe(ErrorObject error) {
        return error.getDescription() == null ? "" : " (" + error.getDescription() + ")";
    }

    private static String head(String payload) {
        if (payload == null || payload.isBlank()) {
            return "(empty body)";
        }
        String trimmed = payload.strip();
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120) + "…";
    }
}
