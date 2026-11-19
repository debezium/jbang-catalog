/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

/**
 * The subset of an OpenID Provider's discovery document this CLI uses.
 * <p>
 * Always obtained from {@code .well-known/openid-configuration} rather than composed from the issuer
 * by string concatenation — that is what lets the same CLI work against Keycloak, Entra or Cognito
 * without a code change.
 */
public record OidcEndpoints(String issuer,
        String deviceAuthorizationEndpoint,
        String tokenEndpoint,
        String endSessionEndpoint) {
}
