/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.debezium.jbang.configuration.Configuration;

class BuildCommandNetworkIT {

    private static final String TEST_VERSION = "3.7.0.Alpha2";

    private static Configuration loadDefaultConfiguration() {
        try (InputStream is = BuildCommandNetworkIT.class.getClassLoader().getResourceAsStream("config-defaults.yaml")) {
            return new ObjectMapper(new YAMLFactory()).readValue(is, Configuration.class);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BuildCommand silentCommand(String configPath) {
        BuildCommand cmd = new BuildCommand(null) {
            @Override
            public void println(String line) {
            }

            @Override
            public void println() {
            }

            @Override
            public void print(String output) {
            }

            @Override
            public void printf(String format, Object... args) {
            }

            @Override
            public Configuration configuration() {
                return loadDefaultConfiguration();
            }
        };
        cmd.configPath = configPath;
        return cmd;
    }

    @Test
    void buildPostgresToKafkaResolvesDistributionZip(@TempDir Path tempDir) throws Exception {
        Path cfg = tempDir.resolve("dbz.yaml");
        Files.writeString(cfg, "version: \"" + TEST_VERSION + "\"\nsource:\n  type: postgres\nsink:\n  type: kafka\n");

        int result = silentCommand(cfg.toString()).doCall();

        assertThat(result).isEqualTo(0);
        Path zipPath = Path.of("target", "debezium-server-" + TEST_VERSION + ".zip");
        assertThat(zipPath).exists();
        assertThat(countJarsInZip(zipPath)).isGreaterThan(0);
    }

    @Test
    void buildMysqlToKafkaResolvesDistributionZip(@TempDir Path tempDir) throws Exception {
        Path cfg = tempDir.resolve("dbz.yaml");
        Files.writeString(cfg, "version: \"" + TEST_VERSION + "\"\nsource:\n  type: mysql\nsink:\n  type: kafka\n");

        int result = silentCommand(cfg.toString()).doCall();

        assertThat(result).isEqualTo(0);
        Path zipPath = Path.of("target", "debezium-server-" + TEST_VERSION + ".zip");
        assertThat(zipPath).exists();
        assertThat(countJarsInZip(zipPath)).isGreaterThan(0);
    }

    @Test
    void buildWithImageFlagProducesTar(@TempDir Path tempDir) throws Exception {
        Path cfg = tempDir.resolve("dbz.yaml");
        Files.writeString(cfg,
                "version: \"" + TEST_VERSION + "\"\n"
                        + "source:\n  type: postgres\n"
                        + "sink:\n  type: kafka\n"
                        + "build:\n  image:\n    name: debezium-test\n    tag: it-test\n");

        BuildCommand cmd = silentCommand(cfg.toString());
        cmd.buildImage = true;
        int result = cmd.doCall();

        assertThat(result).isEqualTo(0);
        assertThat(Path.of("target", "debezium-server.tar")).exists();
    }

    @Test
    void resolvedZipContainsSinkProfileJar(@TempDir Path tempDir) throws Exception {
        Path cfg = tempDir.resolve("dbz.yaml");
        Files.writeString(cfg, "version: \"" + TEST_VERSION + "\"\nsource:\n  type: postgres\nsink:\n  type: kafka\n");

        int result = silentCommand(cfg.toString()).doCall();

        assertThat(result).isEqualTo(0);
        Path zipPath = Path.of("target", "debezium-server-" + TEST_VERSION + ".zip");
        List<String> jarNames = listJarNamesInZip(zipPath);
        assertThat(jarNames).anyMatch(name -> name.contains("debezium-server-kafka"));
    }

    @Test
    void resolvedZipJarsAreUnderLibPath(@TempDir Path tempDir) throws Exception {
        Path cfg = tempDir.resolve("dbz.yaml");
        Files.writeString(cfg, "version: \"" + TEST_VERSION + "\"\nsource:\n  type: postgres\nsink:\n  type: kafka\n");

        int result = silentCommand(cfg.toString()).doCall();

        assertThat(result).isEqualTo(0);
        Path zipPath = Path.of("target", "debezium-server-" + TEST_VERSION + ".zip");
        try (InputStream fis = Files.newInputStream(zipPath);
                ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".jar")) {
                    assertThat(entry.getName()).startsWith("debezium-server");
                }
                zis.closeEntry();
            }
        }
    }

    private int countJarsInZip(Path zipPath) throws Exception {
        int count = 0;
        try (InputStream fis = Files.newInputStream(zipPath);
                ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".jar")) {
                    count++;
                }
                zis.closeEntry();
            }
        }
        return count;
    }

    private List<String> listJarNamesInZip(Path zipPath) throws Exception {
        List<String> names = new ArrayList<>();
        try (InputStream fis = Files.newInputStream(zipPath);
                ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".jar")) {
                    names.add(Path.of(entry.getName()).getFileName().toString());
                }
                zis.closeEntry();
            }
        }
        return names;
    }
}
