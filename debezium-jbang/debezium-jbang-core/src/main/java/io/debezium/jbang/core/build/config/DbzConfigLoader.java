/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.build.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class DbzConfigLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private DbzConfigLoader() {
    }

    public static DbzConfig load(Path path) throws IOException {
        String content = Files.readString(path);
        String resolved = resolveEnvVars(content);
        return MAPPER.readValue(resolved, DbzConfig.class);
    }

    public static DbzConfig loadFromCurrentDir() throws IOException {
        return load(Path.of("dbz.yaml"));
    }

    static String resolveEnvVars(String content) {
        Matcher matcher = ENV_VAR_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = System.getenv(varName);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : matcher.group(0)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String loadTemplate(String source, String sink) {
        String templateName = source + "-" + sink + ".yaml";
        try (InputStream is = DbzConfigLoader.class.getResourceAsStream("/templates/init/" + templateName)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        catch (IOException ignore) {
        }
        // Fall back to generic template with placeholder substitution
        try (InputStream is = DbzConfigLoader.class.getResourceAsStream("/templates/init/generic.yaml")) {
            if (is != null) {
                String generic = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return generic.replace("{source_type}", source).replace("{sink_type}", sink);
            }
        }
        catch (IOException ignore) {
        }
        return null;
    }
}
