/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.build.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvironmentVariableResolver {

    public static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public static String resolve(String text) {
        Matcher matcher = ENV_VAR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = System.getenv(varName);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : matcher.group(0)));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static Matcher matcher(String text) {
        return ENV_VAR_PATTERN.matcher(text);
    }
}
