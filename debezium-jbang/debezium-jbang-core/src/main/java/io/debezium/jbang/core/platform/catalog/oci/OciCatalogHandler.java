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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.jbang.core.platform.catalog.Catalog;
import io.debezium.jbang.core.platform.catalog.dto.CatalogManifest;
import io.debezium.jbang.core.platform.catalog.dto.Component;
import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;

public class OciCatalogHandler {

    private static final String MANIFEST_FILE_NAME = "manifest.json";
    private static final String COMPONENTS_FIELD = "components";
    private static final String DESCRIPTOR_SUFFIX = ".json";

    private final Path descriptorsRoot;
    private final String version;
    private final ObjectMapper objectMapper;

    public OciCatalogHandler(Path descriptorsRoot, ObjectMapper objectMapper) {
        this.descriptorsRoot = descriptorsRoot;
        this.version = latestVersion(descriptorsRoot);
        this.objectMapper = objectMapper;
    }

    /**
     * The version directory this service reads from.
     */
    public String version() {
        return version;
    }

    public Catalog getCatalog() {
        Path manifestPath = descriptorsRoot.resolve(version).resolve(MANIFEST_FILE_NAME);
        if (!Files.exists(manifestPath)) {
            throw new IllegalStateException("Catalog manifest not found for version " + version + " at " + manifestPath);
        }
        try {
            CatalogManifest catalogManifest = objectMapper.readValue(manifestPath.toFile(), CatalogManifest.class);

            return new Catalog(
                    new Catalog.Schema(catalogManifest.schemaVersion()),
                    new Catalog.Build(catalogManifest.build().version()),
                    catalogManifest.components()
                            .entrySet()
                            .stream()
                            .map(components -> Map.entry(components.getKey(),
                                    components
                                            .getValue()
                                            .stream()
                                            .map(component -> new Catalog.Component(
                                                    component.componentClass(),
                                                    component.name(),
                                                    component.description(),
                                                    component.descriptor()))
                                            .toList()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to read catalog manifest " + manifestPath, e);
        }
    }

    public Catalog getCatalogByComponentType(String type) {
        Path manifestPath = descriptorsRoot.resolve(version).resolve(MANIFEST_FILE_NAME);
        if (!Files.exists(manifestPath)) {
            throw new IllegalStateException("Catalog manifest not found for version " + version + " at " + manifestPath);
        }

        try {
            CatalogManifest catalogManifest = objectMapper.readValue(manifestPath.toFile(), CatalogManifest.class);

            List<Component> components = catalogManifest
                    .components()
                    .get(type);

            return new Catalog(
                    new Catalog.Schema(catalogManifest.schemaVersion()),
                    new Catalog.Build(catalogManifest.build().version()),
                    Map.of(type, components
                            .stream()
                            .map(component -> new Catalog.Component(component.componentClass(),
                                    component.name(),
                                    component.description(),
                                    component.descriptor()))
                            .toList()));
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to read catalog manifest " + manifestPath, e);
        }
    }

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
