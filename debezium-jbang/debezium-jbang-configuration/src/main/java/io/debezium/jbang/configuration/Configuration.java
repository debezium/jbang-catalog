/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.configuration;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonMerge;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    public static final String DEFAULT_PLATFORM_URL = "http://localhost:8080";
    private static final String ENV_VARIABLE = "DEBEZIUM_PLATFORM_URL";
    private static final String ISSUER_ENV_VARIABLE = "DEBEZIUM_OIDC_ISSUER";
    private static final String CLIENT_ID_ENV_VARIABLE = "DEBEZIUM_OIDC_CLIENT_ID";

    @JsonMerge
    private Platform platform;

    @JsonMerge
    private Build build;

    @JsonMerge
    private Oci oci;

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Build getBuild() {
        return build;
    }

    public void setBuild(Build build) {
        this.build = build;
    }

    public Oci getOci() {
        return oci;
    }

    public void setOci(Oci oci) {
        this.oci = oci;
    }

    @JsonIgnore
    public URI getResolvedPlatform() {
        String envUrl = System.getenv(ENV_VARIABLE);
        if (envUrl != null && !envUrl.isBlank()) {
            return URI.create(envUrl);
        }

        if (platform != null && platform.hasEnvironments()) {
            Environment env = platform.findActiveEnvironment();
            if (env != null && env.getUrl() != null && !env.getUrl().isBlank()) {
                return URI.create(env.getUrl());
            }
        }

        return URI.create(DEFAULT_PLATFORM_URL);
    }

    /**
     * The OIDC issuer for the active environment, or null when none is configured.
     * <p>
     * Same precedence as the platform URL — environment variable first, then the active entry in
     * {@code ~/.dbz/config.yaml} — so that overriding one does not silently leave the other pointing
     * somewhere else.
     */
    @JsonIgnore
    public String getResolvedIssuer() {
        String envIssuer = System.getenv(ISSUER_ENV_VARIABLE);
        if (envIssuer != null && !envIssuer.isBlank()) {
            return envIssuer;
        }
        Environment active = platform == null ? null : platform.findActiveEnvironment();
        return active == null || active.getIssuer() == null || active.getIssuer().isBlank()
                ? null
                : active.getIssuer();
    }

    /** The OAuth client id for the active environment, or null to let the caller default it. */
    @JsonIgnore
    public String getResolvedClientId() {
        String envClientId = System.getenv(CLIENT_ID_ENV_VARIABLE);
        if (envClientId != null && !envClientId.isBlank()) {
            return envClientId;
        }
        Environment active = platform == null ? null : platform.findActiveEnvironment();
        return active == null || active.getClientId() == null || active.getClientId().isBlank()
                ? null
                : active.getClientId();
    }
}
