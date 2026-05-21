/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.util;

public class StringUtil {

    public static String after(String text, String after) {
        if (text == null) {
            return null;
        }
        int pos = text.indexOf(after);
        if (pos == -1) {
            return null;
        }
        return text.substring(pos + after.length());
    }
}
