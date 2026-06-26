/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.transform.service;

import java.util.List;

import io.debezium.jbang.core.platform.transform.dto.TransformRequest;
import io.debezium.jbang.core.platform.transform.dto.TransformResponse;

public interface TransformService {

    List<TransformResponse> listTransforms();

    TransformResponse getTransform(Long id);

    TransformResponse createTransform(TransformRequest transformRequest);

    TransformResponse updateTransform(Long id, TransformRequest transformRequest);

    void deleteTransform(Long id);
}
