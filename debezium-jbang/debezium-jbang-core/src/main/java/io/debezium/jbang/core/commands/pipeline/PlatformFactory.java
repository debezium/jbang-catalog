/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import java.net.URI;

import io.debezium.jbang.core.platform.pipeline.service.HttpPlatformService;
import io.debezium.jbang.core.platform.pipeline.service.PlatformService;
import io.debezium.jbang.core.util.ConfigUtil;

import picocli.CommandLine;

public class PlatformFactory {

    @CommandLine.Option(names = { "--platform-address" }, description = "Debezium Platform API URL (overrides ~/.dbz/config.yaml, default: "
            + ConfigUtil.DEFAULT_PLATFORM_URL + ")", defaultValue = "${DEBEZIUM_PLATFORM_URL:-}")
    private String platformAddress;

    public PlatformService create() {
        if (platformAddress != null && !platformAddress.isBlank()) {
            return new HttpPlatformService(URI.create(platformAddress));
        }
        return new HttpPlatformService(URI.create(ConfigUtil.getPlatformUrl()));
    }
}
