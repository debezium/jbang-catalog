/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * What is persisted for one platform address.
 * <p>
 * The refresh token only — the access token is short-lived and stays in memory. {@code issuer} and
 * {@code clientId} travel with it so that {@code auth status}, {@code auth logout} and every silent
 * refresh work without the user repeating {@code --issuer} on each invocation.
 * <p>
 * {@code refreshTokenType} records the {@code typ} claim at login time: {@code Offline} means the
 * realm actually granted {@code offline_access} and the session will outlive the SSO idle timeout.
 * {@code Refresh} means it will not, which is worth seeing in {@code auth status} before a demo
 * rather than discovering the next morning.
 */
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record StoredCredential(
        @JsonProperty("issuer") String issuer,
        @JsonProperty("clientId") String clientId,
        @JsonProperty("refreshToken") String refreshToken,
        @JsonProperty("subject") String subject,
        @JsonProperty("refreshTokenType") String refreshTokenType,
        @JsonProperty("obtainedAt") String obtainedAt) {
}
