/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.oci;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class CatalogVersions {
    private final List<String> availableVersions;

    public CatalogVersions(List<String> availableVersions) {
        this.availableVersions = availableVersions;
    }

    public List<String> getAvailableVersions() {
        return availableVersions;
    }

    public String acceptedVersion(String version) {
        if (version != null && availableVersions.contains(version)) {
            return version;
        }

        return latestVersion();
    }

    public String latestVersion() {
        return availableVersions.get(availableVersions.size() - 1);
    }

    public static CatalogVersions from(Path descriptorsRoot) {
        if (!Files.isDirectory(descriptorsRoot)) {
            return new CatalogVersions(List.of());
        }
        try (Stream<Path> entries = Files.list(descriptorsRoot)) {
            return new CatalogVersions(entries
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted(versionComparator())
                    .toList());
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to list catalog versions in " + descriptorsRoot, e);
        }
    }

    /**
     * Orders versions ascending by numeric components; for an identical numeric prefix a release
     * ({@code 3.8.0.Final}) sorts after its snapshot ({@code 3.8.0-SNAPSHOT}).
     */
    private static Comparator<String> versionComparator() {
        return (a, b) -> {
            int[] na = numericParts(a);
            int[] nb = numericParts(b);
            int len = Math.max(na.length, nb.length);
            for (int i = 0; i < len; i++) {
                int va = i < na.length ? na[i] : 0;
                int vb = i < nb.length ? nb[i] : 0;
                if (va != vb) {
                    return Integer.compare(va, vb);
                }
            }
            boolean aSnapshot = a.toUpperCase().contains("SNAPSHOT");
            boolean bSnapshot = b.toUpperCase().contains("SNAPSHOT");
            if (aSnapshot != bSnapshot) {
                return aSnapshot ? -1 : 1;
            }
            return a.compareTo(b);
        };
    }

    private static int[] numericParts(String version) {
        String[] tokens = version.split("[.\\-]");
        int[] parts = new int[tokens.length];
        int count = 0;
        for (String token : tokens) {
            if (token.matches("\\d+")) {
                parts[count++] = Integer.parseInt(token);
            }
            else {
                break;
            }
        }
        int[] result = new int[count];
        System.arraycopy(parts, 0, result, 0, count);
        return result;
    }
}
