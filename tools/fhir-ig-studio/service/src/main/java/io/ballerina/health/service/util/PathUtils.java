/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.health.service.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Filesystem helpers shared by the API handlers.
 */
public final class PathUtils {

    private PathUtils() {
    }

    /** Deletes a directory tree, ignoring entries that disappear while walking. */
    public static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(entry -> {
                try {
                    Files.deleteIfExists(entry);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            });
        }
    }

    /**
     * Reduces an arbitrary string to a filesystem and URL safe token.
     */
    public static String slug(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String slug = value.trim().toLowerCase().replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? fallback : slug;
    }
}
