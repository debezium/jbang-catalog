/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.ConfigurationLoader;
import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.build.config.DbzConfig;
import io.debezium.jbang.core.build.config.DbzConfigLoader;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.build.RegistryAuthResolver.RegistryAuth;

import picocli.CommandLine;

@CommandLine.Command(name = "push", description = "Push the assembled OCI image to a container registry", mixinStandardHelpOptions = true, sortOptions = false)
public class PushCommand extends DebeziumCommand {

    @CommandLine.Option(names = { "--config" }, description = "Path to dbz.yaml (default: ./dbz.yaml)", defaultValue = "dbz.yaml")
    String configPath;

    @CommandLine.Option(names = { "--registry" }, description = "Target registry prefix (e.g. ghcr.io/myorg, docker.io/myorg). Overrides dbz.yaml build.image.registry")
    String registryOverride;

    @CommandLine.Option(names = { "--allow-insecure-registries" }, description = "Allow pushing to HTTP (non-TLS) registries such as localhost", defaultValue = "false")
    boolean allowInsecure;

    public PushCommand(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        Path path = Path.of(configPath);
        if (!Files.exists(path)) {
            println("ERROR: " + configPath + " not found. Run 'debezium init' to create one.");
            return 1;
        }

        DbzConfig config;
        try {
            config = DbzConfigLoader.load(path);
        }
        catch (Exception e) {
            println("ERROR: Failed to parse " + configPath + ": " + e.getMessage());
            return 1;
        }

        Path tarPath = Path.of("target", "debezium-server.tar");
        if (!Files.exists(tarPath)) {
            println("ERROR: " + tarPath + " not found. Run 'debezium build --image' first.");
            return 1;
        }

        ResolvedImage resolved = resolveImage(config, registryOverride, ConfigurationLoader.load());
        String fullImageRef = buildFullImageRef(resolved.registry(), resolved.name(), resolved.tag());

        println("Pushing " + resolved.name() + ":" + resolved.tag() + " → " + (resolved.registry() != null ? resolved.registry() : "Docker Hub") + "...");

        Optional<RegistryAuth> auth = RegistryAuthResolver.resolve(resolved.registry());
        if (auth.isEmpty()) {
            println("No credentials found — attempting anonymous push...");
            println("  (If this fails, set DBZ_REGISTRY_USERNAME/DBZ_REGISTRY_PASSWORD or run 'docker login')");
        }

        RegistryImage target = RegistryImage.named(fullImageRef);
        auth.ifPresent(a -> target.addCredential(a.username(), a.password()));

        boolean insecure = allowInsecure || isLocalRegistry(resolved.registry());
        Jib.from(TarImage.at(tarPath).named(resolved.name() + ":" + resolved.tag()))
                .containerize(Containerizer.to(target).setAllowInsecureRegistries(insecure));

        println("Pushed: " + fullImageRef);
        println("Pull with: docker pull " + fullImageRef);
        return 0;
    }

    private record ResolvedImage(String name, String tag, String registry) {
    }

    private static ResolvedImage resolveImage(DbzConfig config, String registryOverride, Configuration globalConfig) {
        String imageName = "debezium-server";
        String imageTag = config.version() != null ? config.version() : "3.7.0.Final";
        String registry = registryOverride;

        if (config.build() != null && config.build().image() != null) {
            DbzConfig.ImageConfig img = config.build().image();
            if (img.name() != null) {
                imageName = img.name();
            }
            if (img.tag() != null) {
                imageTag = img.tag();
            }
            if (registry == null && img.registry() != null) {
                registry = img.registry();
            }
        }
        // Fall back to global config registry if still not set
        if (registry == null && globalConfig != null && globalConfig.getOci() != null
                && globalConfig.getOci().getRegistry() != null) {
            registry = globalConfig.getOci().getRegistry();
        }
        return new ResolvedImage(imageName, imageTag, registry);
    }

    private static boolean isLocalRegistry(String registry) {
        if (registry == null) {
            return false;
        }
        String host = registry.split("/")[0].split(":")[0];
        return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");
    }

    static String buildFullImageRef(String registry, String imageName, String imageTag) {
        if (registry == null || registry.isBlank()) {
            return imageName + ":" + imageTag;
        }
        String base = registry.endsWith("/") ? registry.substring(0, registry.length() - 1) : registry;
        return base + "/" + imageName + ":" + imageTag;
    }
}
