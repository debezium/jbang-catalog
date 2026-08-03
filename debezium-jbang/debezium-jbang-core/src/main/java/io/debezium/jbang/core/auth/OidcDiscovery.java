/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.io.IOException;
import java.net.URI;

import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

/**
 * Reads the OpenID Provider metadata for an issuer.
 * <p>
 * Fetching and parsing {@code .well-known/openid-configuration} is the SDK's job; what stays here is
 * the check that the two endpoints this CLI actually needs are present, with a message that says
 * what to do about it.
 */
public final class OidcDiscovery {

    private OidcDiscovery() {
    }

    public static OidcEndpoints discover(String issuer) {
        if (issuer == null || issuer.isBlank()) {
            throw new AuthException("No OIDC issuer configured. Pass --issuer, set DEBEZIUM_OIDC_ISSUER, "
                    + "or add 'issuer' to the environment in ~/.dbz/config.yaml");
        }
        String base = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;

        OIDCProviderMetadata metadata;
        try {
            metadata = OIDCProviderMetadata.resolve(new Issuer(base));
        }
        catch (GeneralException | IOException e) {
            throw new AuthException("Cannot read the OpenID configuration for " + base
                    + " — is this an OIDC issuer URL? " + e.getMessage(), e);
        }

        URI tokenEndpoint = metadata.getTokenEndpointURI();
        if (tokenEndpoint == null) {
            throw new AuthException("Discovery document for " + base + " has no token_endpoint");
        }
        URI deviceEndpoint = metadata.getDeviceAuthorizationEndpointURI();
        if (deviceEndpoint == null) {
            throw new AuthException("Issuer " + base + " does not advertise device_authorization_endpoint — "
                    + "the device grant is not enabled on this realm");
        }
        URI endSessionEndpoint = metadata.getEndSessionEndpointURI();

        return new OidcEndpoints(
                metadata.getIssuer().getValue(),
                deviceEndpoint.toString(),
                tokenEndpoint.toString(),
                endSessionEndpoint == null ? null : endSessionEndpoint.toString());
    }
}
