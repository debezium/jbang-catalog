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

import io.debezium.jbang.core.commands.version.Version;

import tools.jackson.dataformat.yaml.YAMLMapper;

public class DbzConfigLoader {

    private final YAMLMapper yamlMapper;

    public DbzConfigLoader(YAMLMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    public DbzConfig load(Path path) throws IOException {
        String content = Files.readString(path);
        String resolved = EnvironmentVariableResolver.resolve(content);
        return yamlMapper.readValue(resolved, DbzConfig.class);
    }

    public String loadTemplate(String source, String sink) {
        String cliVersion = Version.getCoreVersion();
        String templateName = source + "-" + sink + ".yaml";
        try (InputStream is = DbzConfigLoader.class.getResourceAsStream("/templates/init/" + templateName)) {
            if (is != null) {
                String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return template.replace("{version}", cliVersion != null ? cliVersion : "");
            }
        }
        catch (IOException ignore) {
        }
        // Fall back to generic template with placeholder substitution
        try (InputStream is = DbzConfigLoader.class.getResourceAsStream("/templates/init/generic.yaml")) {
            if (is != null) {
                String generic = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return generic.replace("{source_type}", source).replace("{sink_type}", sink)
                        .replace("{version}", cliVersion != null ? cliVersion : "");
            }
        }
        catch (IOException ignore) {
        }
        return null;
    }
}
