/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.util.URLUtils;

import net.minidev.json.JSONObject;

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
        String url = endpoints.tokenEndpoint();
        TokenRequest request = new TokenRequest.Builder(
                URI.create(url),
                new ClientID(clientId),
                new RefreshTokenGrant(new RefreshToken(refreshToken))).build();

        JSONObject body = OidcHttp.body(OidcHttp.send(request.toHTTPRequest(), url), url);

        TokenResponse response;
        try {
            response = TokenResponse.parse(body);
        }
        catch (ParseException e) {
            throw new AuthException("Could not read the token response from " + url + " — " + e.getMessage(), e);
        }

        if (response.indicatesSuccess()) {
            return TokenSet.from(response.toSuccessResponse());
        }

        ErrorObject error = response.toErrorResponse().getErrorObject();
        if (OAuth2Error.INVALID_GRANT_CODE.equals(error.getCode())) {
            throw new CredentialExpiredException("Stored session is no longer valid" + OidcHttp.describe(error));
        }
        throw new AuthException("Could not refresh the session: " + error.getCode() + OidcHttp.describe(error));
    }

    /**
     * Ends the session at the authorization server so the offline token stops being usable.
     * <p>
     * Deleting the local file alone leaves a live offline session at the provider — the token is
     * long-lived by design, so only the server can actually end it.
     * <p>
     * This is not an OIDC RP-initiated logout with an {@code id_token_hint}; it is the back-channel
     * form Keycloak accepts, which takes the refresh token directly. There is no typed SDK request
     * for it, so the form is built by hand.
     */
    public static void endSession(OidcEndpoints endpoints, String clientId, String refreshToken) {
        if (endpoints.endSessionEndpoint() == null) {
            throw new AuthException("This issuer does not advertise end_session_endpoint");
        }
        String url = endpoints.endSessionEndpoint();

        Map<String, List<String>> form = new LinkedHashMap<>();
        form.put("client_id", List.of(clientId));
        form.put("refresh_token", List.of(refreshToken));

        HTTPRequest request = new HTTPRequest(HTTPRequest.Method.POST, URI.create(url));
        request.setEntityContentType(ContentType.APPLICATION_URLENCODED);
        request.setBody(URLUtils.serializeParameters(form));

        HTTPResponse response = OidcHttp.send(request, url);

        // A successful back-channel logout answers 204 with no body at all, so success has to be
        // read off the status. Only a failure carries a JSON error object worth reporting.
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            return;
        }
        ErrorObject error = ErrorObject.parse(OidcHttp.body(response, url));
        throw new AuthException("Server-side logout failed: " + error.getCode() + OidcHttp.describe(error));
    }
}
