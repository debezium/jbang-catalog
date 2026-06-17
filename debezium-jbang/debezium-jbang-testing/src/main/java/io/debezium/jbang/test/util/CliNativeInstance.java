/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliNativeInstance implements BeforeAllCallback, AfterAllCallback {

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("win");
    private static final Logger LOG = LoggerFactory.getLogger(CliNativeInstance.class);

    private String nativeImagePath;

    public CliNativeInstance() {
    }

    public String execute(String command) {
        return execute(command, false, false);
    }

    public String execute(String command, Boolean getError, Boolean expectFail) {
        return executeGenericCommand(nativeImagePath + " " + command, getError, expectFail);
    }

    public String executeGenericCommand(String command, Boolean getError, Boolean expectFail) {
        try {
            LOG.debug("Executing command: {}", command);

            ProcessBuilder pb;
            if (IS_WINDOWS) {
                pb = new ProcessBuilder("cmd", "/c", command);
            }
            else {
                pb = new ProcessBuilder("/bin/bash", "-c", command);
            }

            Process process = pb.start();

            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append(System.lineSeparator());
                    }
                    return sb.toString();
                }
                catch (IOException e) {
                    return e.getMessage();
                }
            });

            String stdout;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
                stdout = sb.toString();
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                Assertions.fail(String.format("command '%s' timed out after %d seconds", command, 30));
            }

            String stderr = stderrFuture.join();
            if (process.exitValue() != 0 && expectFail) {
                LOG.debug(String.format("command %s failed with output %s and error %s", command, stdout, stderr));
            }
            else if (process.exitValue() != 0 && !expectFail) {
                Assertions.fail(String.format("command %s failed with output %s and error %s", command, stdout, stderr));
            }
            else if (process.exitValue() == 0 && expectFail) {
                Assertions.fail(String.format("command %s was expected to fail but succeeded with output %s and error %s",
                        command, stdout, stderr));
            }

            if (getError) {
                return stderr;
            }
            else {
                return stdout;
            }
        }
        catch (IOException | InterruptedException e) {
            LOG.error("ERROR running command: {}", command, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        nativeImagePath = System.getProperty("native.image.path");
        if (nativeImagePath == null || nativeImagePath.isEmpty()) {
            Assertions.fail("System property 'native.image.path' is not set. Run with -Pnative to build the native image first.");
        }
        if (!Files.exists(Path.of(nativeImagePath))) {
            Assertions.fail("Native image not found at: " + nativeImagePath + ". Run with -Pnative to build the native image first.");
        }
        LOG.info("Using native image at: {}", nativeImagePath);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
    }
}
