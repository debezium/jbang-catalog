/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.connection.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CollectionTree(List<CatalogNode> catalogs) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogNode(String name, List<SchemaNode> schemas, int totalCollections) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SchemaNode(String name, List<CollectionNode> collections, int collectionCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CollectionNode(String name, String fullyQualifiedName) {
    }
}
