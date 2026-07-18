/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands;

import java.net.URI;
import java.util.List;
import java.util.Map;

import io.debezium.jbang.core.configuration.Configuration;
import io.debezium.jbang.core.configuration.Environment;
import io.debezium.jbang.core.configuration.Platform;
import io.debezium.jbang.core.platform.catalog.service.CatalogService;
import io.debezium.jbang.core.platform.catalog.service.HttpCatalogService;
import io.debezium.jbang.core.platform.connection.service.ConnectionService;
import io.debezium.jbang.core.platform.connection.service.HttpConnectionService;
import io.debezium.jbang.core.platform.destination.service.DestinationService;
import io.debezium.jbang.core.platform.destination.service.HttpDestinationService;
import io.debezium.jbang.core.platform.pipeline.service.HttpPlatformService;
import io.debezium.jbang.core.platform.pipeline.service.PlatformService;
import io.debezium.jbang.core.platform.source.service.HttpSourceService;
import io.debezium.jbang.core.platform.source.service.SourceService;
import io.debezium.jbang.core.platform.transform.service.HttpTransformService;
import io.debezium.jbang.core.platform.transform.service.TransformService;
import io.debezium.jbang.core.platform.version.api.PlatformVersionAPI;
import io.debezium.jbang.core.platform.version.dto.PlatformVersionInfo;
import io.debezium.jbang.core.util.ConfigUtil;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.vertx.core.http.HttpClientOptions;

import picocli.CommandLine;

public class PlatformFactory {

    @CommandLine.Option(names = { "--platform-address" }, description = "Debezium Platform API URL (overrides ~/.dbz/config.yaml, default: "
            + ConfigUtil.DEFAULT_PLATFORM_URL + ")", defaultValue = "${DEBEZIUM_PLATFORM_URL:-}")
    private String platformAddress;

    public PlatformService pipeline() {
        return new HttpPlatformService(resolvedApiUrl());
    }

    public SourceService source() {
        return new HttpSourceService(resolvedApiUrl());
    }

    public DestinationService destination() {
        return new HttpDestinationService(resolvedApiUrl());
    }

    public ConnectionService connection() {
        return new HttpConnectionService(resolvedApiUrl());
    }

    public TransformService transform() {
        return new HttpTransformService(resolvedApiUrl());
    }

    public CatalogService catalog() {
        return new HttpCatalogService(resolvedApiUrl());
    }

    public boolean isPlatformConfigured() {
        if (platformAddress != null && !platformAddress.isBlank()) {
            return true;
        }
        Configuration config = Configuration.load();
        Map<String, Environment> envs = config.getEnvironments();
        if (envs != null && !envs.isEmpty()) {
            return true;
        }
        List<Platform> platforms = config.getPlatforms();
        return platforms != null && !platforms.isEmpty();
    }

    public String getPlatformVersion() {
        if (!isPlatformConfigured()) {
            return null;
        }
        try {
            PlatformVersionAPI api = QuarkusRestClientBuilder.newBuilder()
                    .baseUri(resolvedApiUrl())
                    .httpClientOptions(new HttpClientOptions())
                    .build(PlatformVersionAPI.class);
            PlatformVersionInfo info = api.getInfo();
            if (info != null && info.info() != null) {
                return info.info().version();
            }
        }
        catch (Exception ignore) {
        }
        return null;
    }

    public URI resolvedApiUrl() {
        if (platformAddress != null && !platformAddress.isBlank()) {
            return URI.create(platformAddress);
        }
        return URI.create(ConfigUtil.getPlatformUrl());
    }
}
