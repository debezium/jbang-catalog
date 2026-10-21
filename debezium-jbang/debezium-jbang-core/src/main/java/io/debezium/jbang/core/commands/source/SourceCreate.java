/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.source;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.source.Source;
import io.debezium.jbang.core.platform.source.dto.SourceRequest;
import io.debezium.jbang.core.platform.source.mapper.SourceMapper;
import io.debezium.jbang.core.platform.source.service.SourceService;

import picocli.CommandLine;

@CommandLine.Command(name = "create", description = "Create a new source from a YAML file", sortOptions = false)
public class SourceCreate extends DebeziumCommand {

    @CommandLine.Option(names = { "--file", "-f" }, required = true, description = "Path to source YAML file")
    File file;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public SourceCreate(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        SourceRequest request = yamlMapper.readValue(file, SourceRequest.class);

        SourceService sourceService = platformFactory.source(configuration());
        Source created = SourceMapper.toDomain(sourceService.createSource(request));

        println("Source created with id: " + created.id());
        return 0;
    }
}
