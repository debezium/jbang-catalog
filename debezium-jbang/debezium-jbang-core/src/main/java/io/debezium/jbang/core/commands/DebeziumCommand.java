/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands;

import java.util.concurrent.Callable;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.common.Printer;
import io.debezium.jbang.core.util.StringUtil;

import picocli.CommandLine;

public abstract class DebeziumCommand implements Callable<Integer>, Printer {

    private final DebeziumJBangMain main;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    public DebeziumCommand(DebeziumJBangMain main) {
        this.main = main;
    }

    @Override
    public Integer call() throws Exception {
        replacePlaceholders();
        try {
            return doCall();
        }
        catch (Exception e) {
            spec.commandLine().getErr().println("ERROR: " + e.getMessage());
            return 1;
        }
    }

    @Override
    public void println() {
        getMain().getOut().println();
    }

    @Override
    public void println(String line) {
        getMain().getOut().println(line);
    }

    @Override
    public void print(String output) {
        getMain().getOut().print(output);
    }

    @Override
    public void printf(String format, Object... args) {
        getMain().getOut().printf(format, args);
    }

    public DebeziumJBangMain getMain() {
        return main;
    }

    public abstract Integer doCall() throws Exception;

    private void replacePlaceholders() throws Exception {
        if (spec != null) {
            for (CommandLine.Model.ArgSpec argSpec : spec.args()) {
                var provider = spec.defaultValueProvider();
                String defaultValue = provider != null ? provider.defaultValue(argSpec) : null;
                if (defaultValue != null &&
                        argSpec instanceof CommandLine.Model.OptionSpec optionSpec) {
                    for (String name : optionSpec.names()) {
                        String placeholder = "#" + StringUtil.after(name, "--");
                        Object v = argSpec.getValue();
                        if (v != null &&
                                v.toString().contains(placeholder)) {
                            argSpec.setValue(v.toString().replace(placeholder, defaultValue));
                        }
                    }
                }
            }
        }
    }
}
