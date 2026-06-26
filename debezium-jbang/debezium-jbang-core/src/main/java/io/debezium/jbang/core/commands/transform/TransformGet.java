/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.transform;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.transform.Transform;
import io.debezium.jbang.core.platform.transform.mapper.TransformMapper;
import io.debezium.jbang.core.platform.transform.service.TransformService;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Get a transform by id", sortOptions = false)
public class TransformGet extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Transform id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public TransformGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        TransformService transformService = platformFactory.transform();
        Transform t = TransformMapper.toDomain(transformService.getTransform(id));

        println("""
                ID:          %s
                Name:        %s
                Description: %s
                Type:        %s
                Schema:      %s""".formatted(
                t.id(),
                t.name(),
                t.description() != null ? t.description() : "-",
                t.type() != null ? t.type() : "-",
                t.schema() != null ? t.schema() : "-"));
        return 0;
    }
}
