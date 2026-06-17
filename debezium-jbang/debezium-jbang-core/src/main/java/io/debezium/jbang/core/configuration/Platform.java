/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Platform {

    private String address;
    private String name;

    @JsonProperty("default")
    private boolean isDefault;

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
