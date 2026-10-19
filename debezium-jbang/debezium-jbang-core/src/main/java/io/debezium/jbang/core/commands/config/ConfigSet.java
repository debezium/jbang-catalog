/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.config;

import java.util.LinkedHashMap;

import io.debezium.jbang.configuration.ArtifactConfig;
import io.debezium.jbang.configuration.Base;
import io.debezium.jbang.configuration.Build;
import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.ConfigurationLoader;
import io.debezium.jbang.configuration.Container;
import io.debezium.jbang.configuration.Environment;
import io.debezium.jbang.configuration.Maven;
import io.debezium.jbang.configuration.Oci;
import io.debezium.jbang.configuration.Platform;
import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;

import picocli.CommandLine;

@CommandLine.Command(name = "set", description = "Set a CLI configuration value", sortOptions = false)
public class ConfigSet extends DebeziumCommand {

    private static final String KEY_PLATFORM_URL = "platform-url";
    private static final String KEY_BUILD_CONTAINER_IMAGE = "build.container.base.image";
    private static final String KEY_BUILD_CONTAINER_TAG = "build.container.base.tag";
    private static final String KEY_BUILD_MAVEN_LOCAL = "build.maven.local";
    private static final String KEY_BUILD_MAVEN_REMOTE = "build.maven.remote";
    private static final String KEY_OCI_LOCAL = "oci.local";
    private static final String KEY_OCI_REGISTRY = "oci.registry";
    private static final String KEY_OCI_ARTIFACT_NAME = "oci.artifact.name";
    private static final String KEY_OCI_ARTIFACT_TAG = "oci.artifact.tag";

    @CommandLine.Parameters(index = "0", description = "Configuration key (e.g. platform-url, build.container.base.image, oci.registry)")
    String key;

    @CommandLine.Parameters(index = "1", description = "Value to set")
    String value;

    @CommandLine.Option(names = { "--env" }, description = "Environment name (default: 'default')", defaultValue = "default")
    String env;

    public ConfigSet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Configuration config = configuration();

        switch (key) {
            case KEY_PLATFORM_URL -> setPlatformUrl(config);
            case KEY_BUILD_CONTAINER_IMAGE -> setContainerImage(config);
            case KEY_BUILD_CONTAINER_TAG -> setContainerTag(config);
            case KEY_BUILD_MAVEN_LOCAL -> setMavenLocal(config);
            case KEY_BUILD_MAVEN_REMOTE -> setMavenRemote(config);
            case KEY_OCI_LOCAL -> setOciLocal(config);
            case KEY_OCI_REGISTRY -> setOciRegistry(config);
            case KEY_OCI_ARTIFACT_NAME -> setOciArtifactName(config);
            case KEY_OCI_ARTIFACT_TAG -> setOciArtifactTag(config);
            default -> {
                println("Unknown configuration key: '" + key + "'.");
                println("Supported keys: " + String.join(", ",
                        KEY_PLATFORM_URL, KEY_BUILD_CONTAINER_IMAGE, KEY_BUILD_CONTAINER_TAG,
                        KEY_BUILD_MAVEN_LOCAL, KEY_BUILD_MAVEN_REMOTE,
                        KEY_OCI_LOCAL, KEY_OCI_REGISTRY, KEY_OCI_ARTIFACT_NAME, KEY_OCI_ARTIFACT_TAG));
                return 1;
            }
        }

        save(config);
        println("Set '" + key + "' to: " + value);
        return 0;
    }

    private void setPlatformUrl(Configuration config) {
        Platform platform = ensurePlatform(config);

        Environment envConfig = platform.findEnvironment(env);
        if (envConfig != null) {
            envConfig.setUrl(value);
        }
        else {
            envConfig = new Environment();
            envConfig.setUrl(value);
            if (platform.getEnvironments() == null) {
                platform.setEnvironments(new LinkedHashMap<>());
            }
            platform.getEnvironments().put(env, envConfig);
        }

        if (platform.getActive() == null && platform.getEnvironments().size() == 1) {
            platform.setActive(env);
        }
    }

    private void setContainerImage(Configuration config) {
        ensureContainerBase(config).setImage(value);
    }

    private void setContainerTag(Configuration config) {
        ensureContainerBase(config).setTag(value);
    }

    private void setMavenLocal(Configuration config) {
        ensureMaven(config).setLocal(value);
    }

    private void setMavenRemote(Configuration config) {
        ensureMaven(config).setRemote(value);
    }

    private void setOciLocal(Configuration config) {
        ensureOci(config).setLocal(value);
    }

    private void setOciRegistry(Configuration config) {
        ensureOci(config).setRegistry(value);
    }

    private void setOciArtifactName(Configuration config) {
        ensureArtifact(config).setName(value);
    }

    private void setOciArtifactTag(Configuration config) {
        ensureArtifact(config).setTag(value);
    }

    private Platform ensurePlatform(Configuration config) {
        if (config.getPlatform() == null) {
            config.setPlatform(new Platform());
        }
        return config.getPlatform();
    }

    private Build ensureBuild(Configuration config) {
        if (config.getBuild() == null) {
            config.setBuild(new Build());
        }
        return config.getBuild();
    }

    private Base ensureContainerBase(Configuration config) {
        Build build = ensureBuild(config);
        if (build.getContainer() == null) {
            build.setContainer(new Container());
        }
        if (build.getContainer().getBase() == null) {
            build.getContainer().setBase(new Base());
        }
        return build.getContainer().getBase();
    }

    private Maven ensureMaven(Configuration config) {
        Build build = ensureBuild(config);
        if (build.getMaven() == null) {
            build.setMaven(new Maven());
        }
        return build.getMaven();
    }

    private Oci ensureOci(Configuration config) {
        if (config.getOci() == null) {
            config.setOci(new Oci());
        }
        return config.getOci();
    }

    private ArtifactConfig ensureArtifact(Configuration config) {
        Oci oci = ensureOci(config);
        if (oci.getArtifact() == null) {
            oci.setArtifact(new ArtifactConfig());
        }
        return oci.getArtifact();
    }
}
