/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.configuration;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Platform {

    private String active;
    private Map<String, Environment> environments;

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public Map<String, Environment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Map<String, Environment> environments) {
        this.environments = environments;
    }

    public Environment findEnvironment(String name) {
        if (environments == null) {
            return null;
        }
        return environments.get(name);
    }

    public Environment findActiveEnvironment() {
        if (active != null) {
            Environment env = findEnvironment(active);
            if (env != null) {
                return env;
            }
        }
        if (environments != null && !environments.isEmpty()) {
            return environments.values().iterator().next();
        }
        return null;
    }

    public boolean hasEnvironments() {
        return environments != null && !environments.isEmpty();
    }
}
