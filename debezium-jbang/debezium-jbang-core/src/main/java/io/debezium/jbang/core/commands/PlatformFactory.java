/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands;

import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.Platform;
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

import picocli.CommandLine;

public class PlatformFactory {

    @CommandLine.Option(names = { "--platform-address" }, description = "Debezium Platform API URL (overrides ~/.dbz/config.yaml, default: "
            + Configuration.DEFAULT_PLATFORM_URL + ")", defaultValue = "${DEBEZIUM_PLATFORM_URL:-}")
    private String platformAddress;

    public PlatformService pipeline(Configuration configuration) {
        return new HttpPlatformService(resolvedApiUrl(configuration));
    }

    public SourceService source(Configuration configuration) {
        return new HttpSourceService(resolvedApiUrl(configuration));
    }

    public DestinationService destination(Configuration configuration) {
        return new HttpDestinationService(resolvedApiUrl(configuration));
    }

    public ConnectionService connection(Configuration configuration) {
        return new HttpConnectionService(resolvedApiUrl(configuration));
    }

    public TransformService transform(Configuration configuration) {
        return new HttpTransformService(resolvedApiUrl(configuration));
    }

    public CatalogService catalog(Configuration configuration) {
        return new HttpCatalogService(resolvedApiUrl(configuration));
    }

    public boolean isPlatformConfigured(Configuration configuration) {
        if (platformAddress != null && !platformAddress.isBlank()) {
            return true;
        }
        Platform platform = configuration.getPlatform();
        return platform != null && platform.hasEnvironments();
    }

    public String getPlatformVersion(Configuration configuration) {
        if (!isPlatformConfigured(configuration)) {
            return null;
        }
        try {
            String catalogJson = catalog(configuration).getCatalog(null);
            if (catalogJson != null) {
                JsonNode root = new ObjectMapper().readTree(catalogJson);
                JsonNode build = root.path("build");
                if (!build.isMissingNode()) {
                    String version = build.path("version").asText(null);
                    if (version != null) {
                        StringBuilder sb = new StringBuilder(version);
                        String timestamp = build.path("timestamp").asText(null);
                        String commit = build.path("sourceCommit").asText(null);
                        String branch = build.path("sourceBranch").asText(null);
                        if (timestamp != null || commit != null || branch != null) {
                            sb.append(" (");
                            boolean first = true;
                            if (timestamp != null) {
                                sb.append("built ").append(timestamp);
                                first = false;
                            }
                            if (commit != null) {
                                if (!first) {
                                    sb.append(", ");
                                }
                                sb.append("commit ").append(commit);
                                first = false;
                            }
                            if (branch != null) {
                                if (!first) {
                                    sb.append(", ");
                                }
                                sb.append("branch ").append(branch);
                            }
                            sb.append(")");
                        }
                        return sb.toString();
                    }
                }
            }
        }
        catch (Exception ignore) {
        }
        return null;
    }

    public URI resolvedApiUrl(Configuration configuration) {
        if (platformAddress != null && !platformAddress.isBlank()) {
            return URI.create(platformAddress);
        }
        return configuration.getResolvedPlatform();
    }
}
