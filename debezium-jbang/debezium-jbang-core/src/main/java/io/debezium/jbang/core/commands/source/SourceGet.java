/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.source;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.source.Source;
import io.debezium.jbang.core.platform.source.mapper.SourceMapper;
import io.debezium.jbang.core.platform.source.service.SourceService;

import picocli.CommandLine;

@CommandLine.Command(name = "get", description = "Get a source by id", sortOptions = false)
public class SourceGet extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Source id")
    Long id;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public SourceGet(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        SourceService sourceService = platformFactory.source(configuration());
        Source s = SourceMapper.toDomain(sourceService.getSource(id));

        println("""
                ID:          %s
                Name:        %s
                Description: %s
                Type:        %s
                Schema:      %s
                Connection:  %s""".formatted(
                s.id(),
                s.name(),
                s.description() != null ? s.description() : "-",
                s.type() != null ? s.type() : "-",
                s.schema() != null ? s.schema() : "-",
                s.connection() != null ? s.connection().name() + " (id: " + s.connection().id() + ")" : "-"));
        return 0;
    }
}
