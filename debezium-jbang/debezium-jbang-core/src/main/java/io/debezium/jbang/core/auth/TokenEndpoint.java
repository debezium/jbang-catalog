/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Token endpoint operations that are not part of obtaining the first token.
 */
public final class TokenEndpoint {

    private TokenEndpoint() {
    }

    /**
     * Exchanges a refresh token for a fresh access token.
     * <p>
     * {@code invalid_grant} is the one error worth distinguishing: it means the stored credential is
     * dead — revoked, or an SSO-bound refresh token that outlived the realm's idle timeout — and no
     * amount of retrying will fix it. Every other failure is worth surfacing verbatim.
     */
    public static TokenSet refresh(OidcEndpoints endpoints, String clientId, String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);

        JsonNode response = OidcHttp.postForm(endpoints.tokenEndpoint(), form);
        String error = OidcHttp.text(response, "error");

        if (error == null) {
            return TokenSet.from(response);
        }
        if ("invalid_grant".equals(error)) {
            throw new CredentialExpiredException("Stored session is no longer valid" + description(response));
        }
        throw new AuthException("Could not refresh the session: " + error + description(response));
    }

    /**
     * Ends the session at the authorization server so the offline token stops being usable.
     * <p>
     * Deleting the local file alone leaves a live offline session at the provider — the token is
     * long-lived by design, so only the server can actually end it.
     */
    public static void endSession(OidcEndpoints endpoints, String clientId, String refreshToken) {
        if (endpoints.endSessionEndpoint() == null) {
            throw new AuthException("This issuer does not advertise end_session_endpoint");
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        form.put("refresh_token", refreshToken);

        JsonNode response = OidcHttp.postForm(endpoints.endSessionEndpoint(), form);
        String error = OidcHttp.text(response, "error");
        if (error != null) {
            throw new AuthException("Server-side logout failed: " + error + description(response));
        }
    }

    private static String description(JsonNode response) {
        String detail = OidcHttp.text(response, "error_description");
        return detail == null ? "" : " (" + detail + ")";
    }
}
