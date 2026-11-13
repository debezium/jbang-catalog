/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.oci;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;
import io.debezium.jbang.core.platform.catalog.service.CatalogService;

/**
 * {@link CatalogService} backed by component descriptors extracted from an OCI artifact, laid out the
 * same way the Debezium Platform conductor consumes them:
 *
 * <pre>
 *   &lt;root&gt;/&lt;version&gt;/manifest.json
 *   &lt;root&gt;/&lt;version&gt;/&lt;type&gt;/&lt;class&gt;.json
 * </pre>
 *
 * The newest available version directory is used.
 */
public class OciCatalogService implements CatalogService {

    private static final String MANIFEST_FILE_NAME = "manifest.json";
    private static final String COMPONENTS_FIELD = "components";
    private static final String DESCRIPTOR_SUFFIX = ".json";

    private final Path descriptorsRoot;
    private final String version;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OciCatalogService(Path descriptorsRoot) {
        this.descriptorsRoot = descriptorsRoot;
        this.version = latestVersion(descriptorsRoot);
    }

    /** The version directory this service reads from. */
    public String version() {
        return version;
    }

    @Override
    public String getCatalog(String type) {
        Path manifestPath = descriptorsRoot.resolve(version).resolve(MANIFEST_FILE_NAME);
        if (!Files.exists(manifestPath)) {
            throw new IllegalStateException("Catalog manifest not found for version " + version + " at " + manifestPath);
        }
        try {
            if (type == null || type.isBlank()) {
                return Files.readString(manifestPath);
            }
            return filterByType(manifestPath, type).toString();
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to read catalog manifest " + manifestPath, e);
        }
    }

    @Override
    public ComponentDescriptor getComponentDescriptor(String type, String componentClass) {
        Path descriptorPath = descriptorsRoot.resolve(version).resolve(type).resolve(componentClass + DESCRIPTOR_SUFFIX);
        if (!Files.exists(descriptorPath)) {
            throw new IllegalArgumentException(
                    "Descriptor not found for component " + componentClass + " (type " + type + ", version " + version + ")");
        }
        try {
            return objectMapper.readValue(descriptorPath.toFile(), ComponentDescriptor.class);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to read descriptor " + descriptorPath, e);
        }
    }

    private JsonNode filterByType(Path manifestPath, String type) throws IOException {
        JsonNode catalog = objectMapper.readTree(manifestPath.toFile());
        if (catalog instanceof ObjectNode root) {
            JsonNode components = root.get(COMPONENTS_FIELD);
            if (components != null && components.isObject()) {
                ObjectNode filtered = objectMapper.createObjectNode();
                if (components.has(type)) {
                    filtered.set(type, components.get(type));
                }
                root.set(COMPONENTS_FIELD, filtered);
            }
        }
        return catalog;
    }

    static String latestVersion(Path descriptorsRoot) {
        List<String> versions = availableVersions(descriptorsRoot);
        if (versions.isEmpty()) {
            throw new IllegalStateException("No catalog versions found in " + descriptorsRoot);
        }
        return versions.get(versions.size() - 1);
    }

    static List<String> availableVersions(Path descriptorsRoot) {
        if (!Files.isDirectory(descriptorsRoot)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(descriptorsRoot)) {
            return entries
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted(versionComparator())
                    .toList();
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to list catalog versions in " + descriptorsRoot, e);
        }
    }

    /**
     * Orders versions ascending by numeric components; for an identical numeric prefix a release
     * ({@code 3.8.0.Final}) sorts after its snapshot ({@code 3.8.0-SNAPSHOT}).
     */
    static Comparator<String> versionComparator() {
        return (a, b) -> {
            int[] na = numericParts(a);
            int[] nb = numericParts(b);
            int len = Math.max(na.length, nb.length);
            for (int i = 0; i < len; i++) {
                int va = i < na.length ? na[i] : 0;
                int vb = i < nb.length ? nb[i] : 0;
                if (va != vb) {
                    return Integer.compare(va, vb);
                }
            }
            boolean aSnapshot = a.toUpperCase().contains("SNAPSHOT");
            boolean bSnapshot = b.toUpperCase().contains("SNAPSHOT");
            if (aSnapshot != bSnapshot) {
                return aSnapshot ? -1 : 1;
            }
            return a.compareTo(b);
        };
    }

    private static int[] numericParts(String version) {
        String[] tokens = version.split("[.\\-]");
        int[] parts = new int[tokens.length];
        int count = 0;
        for (String token : tokens) {
            if (token.matches("\\d+")) {
                parts[count++] = Integer.parseInt(token);
            }
            else {
                break;
            }
        }
        int[] result = new int[count];
        System.arraycopy(parts, 0, result, 0, count);
        return result;
    }
}
