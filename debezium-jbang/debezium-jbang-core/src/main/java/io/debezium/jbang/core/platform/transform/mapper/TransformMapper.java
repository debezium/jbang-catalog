/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.transform.mapper;

import java.util.List;
import java.util.stream.Collectors;

import io.debezium.jbang.core.platform.transform.Transform;
import io.debezium.jbang.core.platform.transform.dto.TransformResponse;

public class TransformMapper {

    private TransformMapper() {
    }

    public static Transform toDomain(TransformResponse response) {
        return new Transform(
                response.id(),
                response.name(),
                response.description(),
                response.type(),
                response.schema(),
                response.vaults() != null ? response.vaults().stream()
                        .map(v -> new Transform.NamedRef(v.id(), v.name()))
                        .collect(Collectors.toSet()) : null,
                response.config(),
                response.predicate() != null
                        ? new Transform.Predicate(response.predicate().type(), response.predicate().config(), response.predicate().negate())
                        : null);
    }

    public static List<Transform> toDomain(List<TransformResponse> responses) {
        return responses.stream().map(TransformMapper::toDomain).toList();
    }
}
