/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.configuration;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonMerge;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    public static final String DEFAULT_PLATFORM_URL = "http://localhost:8080";

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

    public URI getResolvedPlatform() {
        String envUrl = System.getenv("DEBEZIUM_PLATFORM_URL");
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
}
