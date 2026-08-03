/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Reads the OpenID Provider metadata for an issuer.
 */
public final class OidcDiscovery {

    private static final String WELL_KNOWN = "/.well-known/openid-configuration";

    private OidcDiscovery() {
    }

    public static OidcEndpoints discover(String issuer) {
        if (issuer == null || issuer.isBlank()) {
            throw new AuthException("No OIDC issuer configured. Pass --issuer, set DEBEZIUM_OIDC_ISSUER, "
                    + "or add 'issuer' to the environment in ~/.dbz/config.yaml");
        }
        String base = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        JsonNode document = OidcHttp.get(base + WELL_KNOWN);

        String deviceEndpoint = OidcHttp.text(document, "device_authorization_endpoint");
        String tokenEndpoint = OidcHttp.text(document, "token_endpoint");


        if (tokenEndpoint == null) {
            throw new AuthException("Discovery document at " + base + WELL_KNOWN + " has no token_endpoint");
        }
        if (deviceEndpoint == null) {
            throw new AuthException("Issuer " + base + " does not advertise device_authorization_endpoint — "
                    + "the device grant is not enabled on this realm");
        }

        return new OidcEndpoints(
                OidcHttp.text(document, "issuer") != null ? OidcHttp.text(document, "issuer") : base,
                deviceEndpoint,
                tokenEndpoint,
                OidcHttp.text(document, "end_session_endpoint"));
    }
}
