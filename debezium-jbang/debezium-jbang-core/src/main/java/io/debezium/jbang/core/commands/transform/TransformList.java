/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.transform;

import java.util.List;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.transform.Transform;
import io.debezium.jbang.core.platform.transform.mapper.TransformMapper;
import io.debezium.jbang.core.platform.transform.service.TransformService;

import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List all transforms", sortOptions = false)
public class TransformList extends DebeziumCommand {

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public TransformList(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() {
        TransformService transformService = platformFactory.transform();
        List<Transform> transforms = TransformMapper.toDomain(transformService.listTransforms());

        if (transforms.isEmpty()) {
            println("No transforms found.");
            return 0;
        }

        printf("%-5s %-30s %-20s %-20s%n", "ID", "NAME", "TYPE", "SCHEMA");
        printf("%-5s %-30s %-20s %-20s%n", "---", "----", "----", "------");
        for (Transform t : transforms) {
            printf("%-5s %-30s %-20s %-20s%n",
                    t.id(),
                    t.name(),
                    t.type() != null ? t.type() : "-",
                    t.schema() != null ? t.schema() : "-");
        }
        return 0;
    }
}
