/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.source;

import java.util.List;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.source.Source;
import io.debezium.jbang.core.platform.source.mapper.SourceMapper;
import io.debezium.jbang.core.platform.source.service.SourceService;

import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List all sources", sortOptions = false)
public class SourceList extends DebeziumCommand {

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public SourceList(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() {
        SourceService sourceService = platformFactory.source(configuration());
        List<Source> sources = SourceMapper.toDomain(sourceService.listSources());

        if (sources.isEmpty()) {
            println("No sources found.");
            return 0;
        }

        printf("%-5s %-30s %-20s %-20s%n", "ID", "NAME", "TYPE", "SCHEMA");
        printf("%-5s %-30s %-20s %-20s%n", "---", "----", "----", "------");
        for (Source s : sources) {
            printf("%-5s %-30s %-20s %-20s%n",
                    s.id(),
                    s.name(),
                    s.type() != null ? s.type() : "-",
                    s.schema() != null ? s.schema() : "-");
        }
        return 0;
    }
}
