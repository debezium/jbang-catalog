/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * Attaches {@code Authorization: Bearer …} to platform API calls, and turns a 401 into a message
 * that names the command to run.
 * <p>
 * One instance is bound to one platform address, which is also the credentials key — so the same
 * object that knows where the request is going knows which token belongs to it.
 */
public class BearerTokenFilter implements ClientRequestFilter, ClientResponseFilter {

    private final String platformAddress;

    public BearerTokenFilter(String platformAddress) {
        this.platformAddress = platformAddress;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        // Absent credentials are not an error: an unauthenticated local conductor must keep working
        // as it did before auth existed. Send the request bare and let the server decide.
        String token = TokenProvider.accessTokenOrNull(platformAddress);
        if (token != null) {
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (responseContext.getStatus() != Response.Status.UNAUTHORIZED.getStatusCode()) {
            return;
        }

        // Drop the cached access token: whatever we hold was rejected, so the next invocation should
        // derive a fresh one rather than replay it.
        TokenProvider.invalidate(platformAddress);

        throw new AuthException(TokenProvider.hasCredential(platformAddress)
                ? "The platform rejected this session (401). Run 'debezium auth login --platform-address "
                        + platformAddress + "' to sign in again."
                : "Not authenticated (401). Run 'debezium auth login --platform-address "
                        + platformAddress + "' first.");
    }
}
