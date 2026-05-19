/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands;

import java.util.concurrent.Callable;

import io.debezium.jbang.core.commands.version.VersionCommand;
import io.debezium.jbang.core.common.Printer;

import picocli.CommandLine;

@CommandLine.Command(name = "debezium", description = "Debezium CLI", mixinStandardHelpOptions = true)
public class DebeziumJBangMain implements Callable<Integer> {
    private static CommandLine commandLine;

    private Printer out = new Printer.SystemOutPrinter();

    public static void run(String... args) {
        run(new DebeziumJBangMain(), args);
    }

    private static void run(DebeziumJBangMain main, String... args) {
        main.execute(args);
    }

    private void execute(String... args) {
        try {
            long pid = ProcessHandle.current().pid();
            System.setProperty("pid", Long.toString(pid));
        }
        catch (Exception e) {
            // ignore
        }

        commandLine = new CommandLine(this)
                .addSubcommand("version", new CommandLine(new VersionCommand(this)));

        int exitCode = commandLine.execute(args);
        quit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        commandLine.execute("--help");
        return 0;
    }

    public Printer getOut() {
        return out;
    }

    /**
     * Finish this main with given exit code. By default, uses system exit to terminate. Subclasses may want to
     * overwrite this exit behavior e.g. during unit tests.
     */
    public void quit(int exitCode) {
        System.exit(exitCode);
    }
}
