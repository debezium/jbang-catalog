/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliLocalJBangInstance implements BeforeAllCallback, AfterAllCallback {

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("win");
    private String jbangVersion = null;
    private static final Logger LOG = LoggerFactory.getLogger(CliLocalJBangInstance.class);

    public CliLocalJBangInstance() {
    }

    public String execute(String command) {
        return execute(command, false, false);
    }

    public String execute(String command, Boolean getError, Boolean expectFail) {
        return executeGenericCommand(getMainCommand() + System.getProperty("user.dir") + "/../debezium-jbang-main/dist/DebeziumJBang.java " + command,
                getError,
                expectFail);
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

            // Prepend ~/.jbang/bin to PATH or sdkman!
            String jbangBin = Path.of(System.getProperty("user.home"), ".jbang", "bin").toString();
            String sdkmanBin = Path.of(System.getProperty("user.home"), ".sdkman", "candidates", "jbang", "current", "bin").toString();
            String currentPath = pb.environment().getOrDefault("PATH", "");

            pb.environment().put("PATH", jbangBin + File.pathSeparator + sdkmanBin + File.pathSeparator + currentPath);

            Process process = pb.start();

            // Read stderr concurrently to avoid deadlocks
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
            // Expected to fail and failed
            if (process.exitValue() != 0 && expectFail) {
                LOG.debug(String.format("command %s failed with output %s and error %s", command, stdout, stderr));
                // Expected to succeed but failed
            }
            else if (process.exitValue() != 0 && !expectFail) {
                Assertions.fail(String.format("command %s failed with output %s and error %s", command, stdout, stderr));
                // Expected to fail but succeeded
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

    public String getJBangVersion() {
        return jbangVersion;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!isJBangInstalled()) {
            installJBang();
        }
        jbangVersion = executeGenericCommand(getMainCommand() + " version", false, false);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {

    }

    private String getMainCommand() {
        return System.getProperty(CliProperties.CLI_SERVICE_COMMAND, "jbang ");
    }

    private void installJBang() {
        LOG.info("Installing JBang");
        try {
            ProcessBuilder pb;
            if (IS_WINDOWS) {
                String script = "iex \"& { $(iwr -useb https://ps.jbang.dev) } app setup\"";
                String encoded = Base64.getEncoder().encodeToString(
                        script.getBytes(StandardCharsets.UTF_16LE));
                pb = new ProcessBuilder("powershell", "-NoProfile", "-EncodedCommand", encoded);
            }
            else {
                pb = new ProcessBuilder(
                        "/bin/bash", "-c",
                        "curl -Ls https://sh.jbang.dev | bash -s - app setup");
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
                output = sb.toString();
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                Assertions.fail("JBang installation timed out");
            }
            if (process.exitValue() != 0) {
                Assertions.fail("JBang installation failed: " + output);
            }
            LOG.info("JBang installed successfully");
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to install JBang", e);
        }
    }

    private boolean isJBangInstalled() {
        try {
            String jbangBin = Path.of(System.getProperty("user.home"), ".jbang", "bin").toString();
            String sdkManJbangBin = Path.of(System.getProperty("user.home"), ".sdkman", "candidates", "jbang", "current", "bin").toString();
            String pathSeparator = IS_WINDOWS ? ";" : ":";
            ProcessBuilder pb = IS_WINDOWS
                    ? new ProcessBuilder("cmd", "/c", "jbang version")
                    : new ProcessBuilder("/bin/bash", "-c", "jbang version");
            String currentPath = pb.environment().getOrDefault("PATH", "");
            pb.environment().put("PATH", jbangBin + pathSeparator + sdkManJbangBin + pathSeparator + currentPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            boolean installed = process.exitValue() == 0;
            LOG.info("JBang installed: {}", installed);
            return installed;
        }
        catch (IOException | InterruptedException e) {
            LOG.info("JBang not found: {}", e.getMessage());
            return false;
        }
    }
}
