/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PlatformTest {

    @Test
    void shouldFindEnvironmentByName() {
        Platform platform = createPlatform("default", "http://localhost:8080");

        assertThat(platform.findEnvironment("default")).isNotNull();
        assertThat(platform.findEnvironment("default").getUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void shouldReturnNullForUnknownEnvironment() {
        Platform platform = createPlatform("default", "http://localhost:8080");

        assertThat(platform.findEnvironment("nonexistent")).isNull();
    }

    @Test
    void shouldReturnNullWhenNoEnvironments() {
        Platform platform = new Platform();

        assertThat(platform.findEnvironment("any")).isNull();
        assertThat(platform.findActiveEnvironment()).isNull();
        assertThat(platform.hasEnvironments()).isFalse();
    }

    @Test
    void shouldFindActiveEnvironment() {
        Platform platform = new Platform();
        platform.setActive("staging");

        Map<String, Environment> envs = new LinkedHashMap<>();
        envs.put("default", env("http://localhost:8080"));
        envs.put("staging", env("http://staging:9090"));
        platform.setEnvironments(envs);

        Environment active = platform.findActiveEnvironment();
        assertThat(active).isNotNull();
        assertThat(active.getUrl()).isEqualTo("http://staging:9090");
    }

    @Test
    void shouldFallBackToFirstEnvironmentWhenActiveNotSet() {
        Platform platform = new Platform();
        Map<String, Environment> envs = new LinkedHashMap<>();
        envs.put("first", env("http://first:8080"));
        envs.put("second", env("http://second:8080"));
        platform.setEnvironments(envs);

        Environment active = platform.findActiveEnvironment();
        assertThat(active).isNotNull();
        assertThat(active.getUrl()).isEqualTo("http://first:8080");
    }

    @Test
    void shouldFallBackToFirstEnvironmentWhenActiveDoesNotExist() {
        Platform platform = new Platform();
        platform.setActive("deleted-env");
        Map<String, Environment> envs = new LinkedHashMap<>();
        envs.put("only", env("http://only:8080"));
        platform.setEnvironments(envs);

        Environment active = platform.findActiveEnvironment();
        assertThat(active).isNotNull();
        assertThat(active.getUrl()).isEqualTo("http://only:8080");
    }

    @Test
    void shouldReportHasEnvironments() {
        Platform platform = createPlatform("default", "http://localhost:8080");

        assertThat(platform.hasEnvironments()).isTrue();
    }

    private static Platform createPlatform(String name, String url) {
        Platform platform = new Platform();
        Map<String, Environment> envs = new LinkedHashMap<>();
        envs.put(name, env(url));
        platform.setEnvironments(envs);
        platform.setActive(name);
        return platform;
    }

    private static Environment env(String url) {
        Environment env = new Environment();
        env.setUrl(url);
        return env;
    }
}
