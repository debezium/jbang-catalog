/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.oci;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.debezium.jbang.core.platform.catalog.Catalog;
import io.debezium.jbang.core.platform.catalog.dto.CatalogManifest;
import io.debezium.jbang.core.platform.catalog.dto.Component;
import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;

import tools.jackson.databind.ObjectMapper;

public class OciCatalogHandler {

    private static final String MANIFEST_FILE_NAME = "manifest.json";
    private static final String DESCRIPTOR_SUFFIX = ".json";

    private final Path descriptorsRoot;
    private final ObjectMapper objectMapper;

    public OciCatalogHandler(Path descriptorsRoot, ObjectMapper objectMapper) {
        this.descriptorsRoot = descriptorsRoot;
        this.objectMapper = objectMapper;
    }

    public Catalog getCatalog(String version) {
        Path manifestPath = descriptorsRoot.resolve(version).resolve(MANIFEST_FILE_NAME);
        if (!Files.exists(manifestPath)) {
            throw new IllegalStateException("Catalog manifest not found for version " + version + " at " + manifestPath);
        }
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

    public Catalog getCatalogByComponentType(String version, String type) {
        Path manifestPath = descriptorsRoot.resolve(version).resolve(MANIFEST_FILE_NAME);
        if (!Files.exists(manifestPath)) {
            throw new IllegalStateException("Catalog manifest not found for version " + version + " at " + manifestPath);
        }

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

    public ComponentDescriptor getComponentDescriptor(String version, String type, String componentClass) {
        Path descriptorPath = descriptorsRoot.resolve(version).resolve(type).resolve(componentClass + DESCRIPTOR_SUFFIX);
        if (!Files.exists(descriptorPath)) {
            throw new IllegalArgumentException(
                    "Descriptor not found for component " + componentClass + " (type " + type + ", version " + version + ")");
        }
        return objectMapper.readValue(descriptorPath.toFile(), ComponentDescriptor.class);
    }
}
