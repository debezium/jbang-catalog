/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.test.suite;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.debezium.jbang.test.util.CliNativeInstance;

public class NativeTestPicoCliCommand {

    @RegisterExtension
    protected static CliNativeInstance nativeInstance = new CliNativeInstance();

    public String execute(String command) {
        return nativeInstance.execute(command);
    }
}
