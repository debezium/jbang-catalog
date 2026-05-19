/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.test.suite;

import io.debezium.jbang.test.util.CliLocalJBangInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

public class JBangTestPicoCliCommand {

    @RegisterExtension
    protected static CliLocalJBangInstance containerService = new CliLocalJBangInstance();

    public String execute(String command) {
        return containerService.execute(command);
    }

    public CliLocalJBangInstance instance() {
        return containerService;
    }
}
