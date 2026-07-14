/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.source.service;

import java.util.List;

import io.debezium.jbang.core.platform.source.dto.SignalCollectionVerifyRequest;
import io.debezium.jbang.core.platform.source.dto.SignalVerificationResult;
import io.debezium.jbang.core.platform.source.dto.SourceRequest;
import io.debezium.jbang.core.platform.source.dto.SourceResponse;

public interface SourceService {

    List<SourceResponse> listSources();

    SourceResponse getSource(Long id);

    SourceResponse createSource(SourceRequest sourceRequest);

    SourceResponse updateSource(Long id, SourceRequest sourceRequest);

    void deleteSource(Long id);

    SignalVerificationResult verifySignals(SignalCollectionVerifyRequest request);
}
