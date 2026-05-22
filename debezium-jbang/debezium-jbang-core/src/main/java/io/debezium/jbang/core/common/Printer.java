/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.common;

public interface Printer {

    void println();

    void println(String line);

    void print(String output);

    void printf(String format, Object... args);

    class SystemOutPrinter implements Printer {

        @Override
        public void println() {
            // CHECKSTYLE:OFF
            System.out.println();
            System.out.flush();
            // CHECKSTYLE:ON
        }

        @Override
        public void println(String line) {
            // CHECKSTYLE:OFF
            System.out.println(line);
            System.out.flush();
            // CHECKSTYLE:ON
        }

        @Override
        public void print(String output) {
            // CHECKSTYLE:OFF
            System.out.print(output);
            // CHECKSTYLE:ON
        }

        @Override
        public void printf(String format, Object... args) {
            // CHECKSTYLE:OFF
            System.out.printf(format, args);
            // CHECKSTYLE:ON
        }
    }
}
