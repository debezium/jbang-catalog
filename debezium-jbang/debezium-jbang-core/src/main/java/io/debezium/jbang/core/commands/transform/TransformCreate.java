/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.transform;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.transform.Transform;
import io.debezium.jbang.core.platform.transform.dto.TransformRequest;
import io.debezium.jbang.core.platform.transform.mapper.TransformMapper;
import io.debezium.jbang.core.platform.transform.service.TransformService;

import picocli.CommandLine;

@CommandLine.Command(name = "create", description = "Create a new transform from a YAML file", sortOptions = false)
public class TransformCreate extends DebeziumCommand {

    @CommandLine.Option(names = { "--file", "-f" }, required = true, description = "Path to transform YAML file")
    File file;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public TransformCreate(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        TransformRequest request = yamlMapper.readValue(file, TransformRequest.class);

        TransformService transformService = platformFactory.transform();
        Transform created = TransformMapper.toDomain(transformService.createTransform(request));

        println("Transform created with id: " + created.id());
        return 0;
    }
}
