/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonMerge;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

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
}
