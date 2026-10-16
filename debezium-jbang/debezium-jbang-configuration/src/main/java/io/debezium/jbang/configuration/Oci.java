/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonMerge;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Oci {

    private String local;
    private String registry;

    @JsonMerge
    private ArtifactConfig artifact;

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public ArtifactConfig getArtifact() {
        return artifact;
    }

    public void setArtifact(ArtifactConfig artifact) {
        this.artifact = artifact;
    }
}
