/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.debezium.jbang.configuration.Configuration;

public class DistributionResolver {

    private static final String DIST_PATH = "io/debezium/debezium-server-dist";
    private static final String SOURCES_CLASSIFIER = "-sources.jar";

    private final Consumer<String> logger;
    private final Configuration configuration;

    public DistributionResolver(Consumer<String> logger, Configuration configuration) {
        this.logger = logger;
        this.configuration = configuration;
    }

    public Path resolve(String version, List<String> activeProfiles, List<String> explicitArtifacts) throws Exception {
        logger.accept("Preparing debezium-server-dist " + version + " project...");

        Path projectDir = Files.createTempDirectory("debezium-dist-");
        Path srcAssembliesDir = projectDir.resolve("src/main/resources/assemblies");
        Files.createDirectories(srcAssembliesDir);

        // Download POM from Maven repository
        String repoBase = ensureTrailingSlash(configuration.getBuild().getMaven().getRemote());
        String pomUrl = repoBase + DIST_PATH + "/" + version
                + "/debezium-server-dist-" + version + ".pom";
        logger.accept("Fetching POM from " + pomUrl);

        Document doc;
        try (InputStream is = URI.create(pomUrl).toURL().openStream()) {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
        doc.getDocumentElement().normalize();

        // Ensure custom-distribution profile has debezium-server-core, assembly plugin,
        // and explicit connector/sink dependencies (for releases that predate connector profiles)
        ensureCustomDistributionFix(doc, version, explicitArtifacts);

        // Write modified POM to project directory
        Path pomPath = projectDir.resolve("pom.xml");
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        Files.writeString(pomPath, sw.toString(), StandardCharsets.UTF_8);

        // Extract assembly descriptor and distro scripts from the official sources jar
        Path distroDir = projectDir.resolve("src/main/resources/distro");
        Files.createDirectories(distroDir);
        extractFromSourcesJar(version, srcAssembliesDir, distroDir);

        String profiles = String.join(",", activeProfiles);
        logger.accept("Running: mvn package -P " + profiles);

        List<String> command = new ArrayList<>();
        command.add(findMaven());
        command.add("package");
        command.add("-P");
        command.add(profiles);
        command.add("-DskipTests");
        command.add("--no-transfer-progress");
        String localRepo = configuration.getBuild().getMaven().getLocal();
        if (localRepo != null && !localRepo.isBlank()) {
            if (localRepo.startsWith("~")) {
                localRepo = System.getProperty("user.home") + localRepo.substring(1);
            }
            command.add("-Dmaven.repo.local=" + localRepo);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (InputStream is = process.getInputStream()) {
            byte[] buf = new byte[4096];
            int n;
            StringBuilder line = new StringBuilder();
            while ((n = is.read(buf)) != -1) {
                String chunk = new String(buf, 0, n);
                for (char c : chunk.toCharArray()) {
                    if (c == '\n') {
                        String l = line.toString().trim();
                        if (!l.isEmpty()) {
                            logger.accept(l);
                        }
                        line.setLength(0);
                    }
                    else {
                        line.append(c);
                    }
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Maven build failed (exit " + exitCode + "). See output above.");
        }

        Path targetDir = projectDir.resolve("target");
        Path generatedZip = Files.list(targetDir)
                .filter(p -> p.getFileName().toString().endsWith(".zip"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No zip found in " + targetDir + " after Maven build."));

        Path outputDir = Path.of("target");
        Files.createDirectories(outputDir);
        Path zipPath = outputDir.resolve("debezium-server-" + version + ".zip");
        Files.copy(generatedZip, zipPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        logger.accept("Build complete: " + zipPath);
        return zipPath;
    }

    private void ensureCustomDistributionFix(Document doc, String version, List<String> explicitArtifacts) {
        NodeList profileNodes = doc.getElementsByTagName("profile");
        for (int i = 0; i < profileNodes.getLength(); i++) {
            Node profile = profileNodes.item(i);
            String id = childText(profile, "id");
            if (!"custom-distribution".equals(id)) {
                continue;
            }

            Node dependencies = childNode(profile, "dependencies");
            if (dependencies == null) {
                dependencies = doc.createElement("dependencies");
                profile.appendChild(dependencies);
            }

            // Add debezium-server-core dependency if missing
            if (!hasDependency(dependencies, "debezium-server-core")) {
                Element dep = doc.createElement("dependency");
                appendTextElement(doc, dep, "groupId", "io.debezium");
                appendTextElement(doc, dep, "artifactId", "debezium-server-core");
                dependencies.appendChild(dep);
            }

            // Inject explicit connector/sink artifacts (for older releases without connector profiles)
            for (String artifact : explicitArtifacts) {
                String[] parts = artifact.split(":");
                if (parts.length < 2) {
                    continue;
                }
                String groupId = parts[0];
                String artifactId = parts[1];
                if (!hasDependency(dependencies, artifactId)) {
                    Element dep = doc.createElement("dependency");
                    appendTextElement(doc, dep, "groupId", groupId);
                    appendTextElement(doc, dep, "artifactId", artifactId);
                    appendTextElement(doc, dep, "version", "${project.version}");
                    appendTextElement(doc, dep, "scope", "compile");
                    dependencies.appendChild(dep);
                    logger.accept("Injecting dependency: " + groupId + ":" + artifactId);
                }
            }

            // Add maven-assembly-plugin if missing
            Node build = childNode(profile, "build");
            if (build == null) {
                build = doc.createElement("build");
                profile.appendChild(build);
            }
            Node plugins = childNode(build, "plugins");
            if (plugins == null) {
                plugins = doc.createElement("plugins");
                build.appendChild(plugins);
            }
            if (!hasPlugin(plugins, "maven-assembly-plugin")) {
                plugins.appendChild(buildAssemblyPluginElement(doc));
            }
            return;
        }
    }

    private Element buildAssemblyPluginElement(Document doc) {
        Element plugin = doc.createElement("plugin");
        appendTextElement(doc, plugin, "groupId", "org.apache.maven.plugins");
        appendTextElement(doc, plugin, "artifactId", "maven-assembly-plugin");
        Element executions = doc.createElement("executions");
        Element execution = doc.createElement("execution");
        appendTextElement(doc, execution, "id", "default");
        appendTextElement(doc, execution, "phase", "package");
        Element goals = doc.createElement("goals");
        appendTextElement(doc, goals, "goal", "single");
        execution.appendChild(goals);
        Element configurationElement = doc.createElement("configuration");
        appendTextElement(doc, configurationElement, "appendAssemblyId", "false");
        appendTextElement(doc, configurationElement, "attach", "false");
        Element descriptors = doc.createElement("descriptors");
        appendTextElement(doc, descriptors, "descriptor",
                "src/main/resources/assemblies/server-distribution.xml");
        configurationElement.appendChild(descriptors);
        appendTextElement(doc, configurationElement, "tarLongFileMode", "posix");
        execution.appendChild(configurationElement);
        executions.appendChild(execution);
        plugin.appendChild(executions);
        return plugin;
    }

    private boolean hasDependency(Node dependencies, String artifactId) {
        NodeList children = dependencies.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node dep = children.item(i);
            if ("dependency".equals(dep.getNodeName())) {
                String a = childText(dep, "artifactId");
                if (artifactId.equals(a)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasPlugin(Node plugins, String artifactId) {
        NodeList children = plugins.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node plugin = children.item(i);
            if ("plugin".equals(plugin.getNodeName())) {
                String a = childText(plugin, "artifactId");
                if (artifactId.equals(a)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void appendTextElement(Document doc, Element parent, String tag, String text) {
        Element el = doc.createElement(tag);
        el.setTextContent(text);
        parent.appendChild(el);
    }

    private String childText(Node parent, String tagName) {
        Node child = childNode(parent, tagName);
        return child != null ? child.getTextContent().trim() : null;
    }

    private Node childNode(Node parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (tagName.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    private void extractFromSourcesJar(String version, Path assembliesDir, Path distroDir) throws IOException {
        String sourcesUrl = ensureTrailingSlash(configuration.getBuild().getMaven().getRemote()) + DIST_PATH + "/" + version
                + "/debezium-server-dist-" + version + SOURCES_CLASSIFIER;
        logger.accept("Fetching assembly descriptor and distro scripts from " + sourcesUrl);
        try (InputStream is = URI.create(sourcesUrl).toURL().openStream();
                ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (!entry.isDirectory()) {
                    if (name.startsWith("distro/")) {
                        String relative = name.substring("distro/".length());
                        Path dest = distroDir.resolve(relative);
                        Files.createDirectories(dest.getParent());
                        Files.copy(zis, dest);
                    }
                    else if (name.startsWith("assemblies/")) {
                        String relative = name.substring("assemblies/".length());
                        Path dest = assembliesDir.resolve(relative);
                        Files.createDirectories(dest.getParent());
                        Files.copy(zis, dest);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static String ensureTrailingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private static String findMaven() {
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome != null && !mavenHome.isBlank()) {
            Path mvn = Path.of(mavenHome, "bin", "mvn");
            if (Files.exists(mvn)) {
                return mvn.toString();
            }
        }
        return "mvn";
    }
}
