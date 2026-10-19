/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;

import io.debezium.jbang.configuration.Configuration;
import io.debezium.jbang.configuration.ConfigurationLoader;
import io.debezium.jbang.connectors.ConnectorRegistry;
import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.build.config.DbzConfig;
import io.debezium.jbang.core.build.config.DbzConfigLoader;
import io.debezium.jbang.core.commands.DebeziumCommand;

import picocli.CommandLine;

@CommandLine.Command(name = "build", description = "Build a Debezium Server distribution from dbz.yaml", mixinStandardHelpOptions = true, sortOptions = false)
public class BuildCommand extends DebeziumCommand {

    private static final String DEFAULT_BASE_IMAGE = "eclipse-temurin:21-jre"; // fallback only if config loading fails
    private static final AbsoluteUnixPath APP_DIR = AbsoluteUnixPath.get("/app");

    @CommandLine.Option(names = { "--config" }, description = "Path to dbz.yaml (default: ./dbz.yaml)", defaultValue = "dbz.yaml")
    String configPath;

    @CommandLine.Option(names = { "--image" }, description = "Assemble a layered OCI image after building", defaultValue = "false")
    boolean buildImage;

    public BuildCommand(DebeziumJBangMain main) {
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

        if (config.source() == null || config.source().type() == null) {
            println("ERROR: source.type is required in " + configPath);
            return 1;
        }
        if (config.sink() == null || config.sink().type() == null) {
            println("ERROR: sink.type is required in " + configPath);
            return 1;
        }

        String sourceType = config.source().type().toLowerCase();
        String sinkType = config.sink().type().toLowerCase();

        ConnectorRegistry.ConnectorEntry connector;
        try {
            connector = ConnectorRegistry.getConnector(sourceType);
        }
        catch (IllegalArgumentException e) {
            println("ERROR: " + e.getMessage());
            return 1;
        }

        ConnectorRegistry.SinkEntry sink;
        try {
            sink = ConnectorRegistry.getSink(sinkType);
        }
        catch (IllegalArgumentException e) {
            println("ERROR: " + e.getMessage());
            return 1;
        }

        String version = config.version() != null ? config.version() : "3.7.0.Final";

        List<String> activeProfiles = new ArrayList<>();
        activeProfiles.add("custom-distribution");
        activeProfiles.add(connector.profile());
        activeProfiles.add(sink.profile());

        if (config.extras() != null) {
            for (String extra : config.extras()) {
                try {
                    activeProfiles.add(ConnectorRegistry.getExtra(extra).profile());
                }
                catch (IllegalArgumentException e) {
                    println("ERROR: " + e.getMessage());
                    return 1;
                }
            }
        }

        println("Building " + sourceType + " -> " + sinkType + " (version " + version + ")...");

        List<String> explicitArtifacts = new ArrayList<>();
        explicitArtifacts.add(connector.artifact());
        explicitArtifacts.add(sink.artifact());

        Path zipPath;
        try {
            zipPath = new DistributionResolver(this::println, configuration()).resolve(version, activeProfiles, explicitArtifacts);
        }
        catch (Exception e) {
            println("ERROR: Failed to resolve distribution: " + e.getMessage());
            return 1;
        }

        println("Distribution ready: " + zipPath);

        if (buildImage) {
            return buildOciImage(config, zipPath, version);
        }

        return 0;
    }

    private int buildOciImage(DbzConfig config, Path zipPath, String version) throws Exception {
        println("Assembling OCI image...");

        Path tempDir = Files.createTempDirectory("debezium-image-");
        List<Path> libJars = unpackLib(zipPath, tempDir);
        Path runnerJar = unpackRunner(zipPath, tempDir);

        List<Path> coreJars = new ArrayList<>();
        List<Path> connectorSinkJars = new ArrayList<>();
        for (Path jar : libJars) {
            String name = jar.getFileName().toString();
            if (name.startsWith("debezium-connector") || name.startsWith("debezium-server-")) {
                connectorSinkJars.add(jar);
            }
            else {
                coreJars.add(jar);
            }
        }

        String imageName = "debezium-server";
        String imageTag = version;
        if (config.build() != null && config.build().image() != null) {
            DbzConfig.ImageConfig img = config.build().image();
            if (img.name() != null) {
                imageName = img.name();
            }
            if (img.tag() != null) {
                imageTag = img.tag();
            }
        }

        Configuration globalConfig = configuration();
        String baseImage = DEFAULT_BASE_IMAGE;
        if (globalConfig.getBuild() != null && globalConfig.getBuild().getContainer() != null
                && globalConfig.getBuild().getContainer().getBase() != null) {
            String img = globalConfig.getBuild().getContainer().getBase().getImage();
            String tag = globalConfig.getBuild().getContainer().getBase().getTag();
            if (img != null && !img.isBlank()) {
                baseImage = tag != null && !tag.isBlank() ? img + ":" + tag : img;
            }
        }

        Path tarOutput = Path.of("target", "debezium-server.tar");

        // Runner JAR goes to /app/, lib JARs to /app/lib/ (split into layers for cache efficiency)
        JibContainerBuilder builder = Jib.from(baseImage)
                .addLayer(List.of(runnerJar), APP_DIR)
                .addLayer(coreJars, APP_DIR.resolve("lib"))
                .addLayer(connectorSinkJars, APP_DIR.resolve("lib"));

        builder.addEnvironmentVariable("DEBEZIUM_SOURCE_TYPE", config.source().type())
                .addEnvironmentVariable("DEBEZIUM_SINK_TYPE", config.sink().type())
                .setEntrypoint(List.of(
                        "java",
                        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                        "-cp", "/app/runner.jar:/app/conf:/app/lib/*",
                        "io.debezium.server.Main"))
                .containerize(Containerizer.to(
                        TarImage.at(tarOutput).named(imageName + ":" + imageTag)));

        println("Image assembled: " + tarOutput + " (" + imageName + ":" + imageTag + ")");
        println("Load with: docker load -i " + tarOutput);
        println("Run with:  docker run --rm -v $(pwd)/application.properties:/app/conf/application.properties " + imageName + ":" + imageTag);
        println("Note: mount your application.properties at runtime — do not bake config into the image (12-factor III)");
        return 0;
    }

    private List<Path> unpackLib(Path zipPath, Path destDir) throws IOException {
        List<Path> jars = new ArrayList<>();
        try (InputStream fis = Files.newInputStream(zipPath);
                ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".jar") && name.contains("/lib/")) {
                    String fileName = Path.of(name).getFileName().toString();
                    Path dest = destDir.resolve(fileName);
                    Files.copy(zis, dest);
                    jars.add(dest);
                }
                zis.closeEntry();
            }
        }
        return jars;
    }

    private Path unpackRunner(Path zipPath, Path destDir) throws IOException {
        try (InputStream fis = Files.newInputStream(zipPath);
                ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith("-runner.jar") && !name.contains("/lib/")) {
                    Path dest = destDir.resolve("runner.jar");
                    Files.copy(zis, dest);
                    return dest;
                }
                zis.closeEntry();
            }
        }
        throw new RuntimeException("No runner jar found in " + zipPath);
    }

}
