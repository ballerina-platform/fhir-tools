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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Builds browsable views of a generated artifact directory.
 */
public final class FileTrees {

    /** Files above this size are listed but not previewable as text. */
    public static final long MAX_PREVIEW_BYTES = 512 * 1024L;

    private FileTrees() {
    }

    /**
     * Describes one entry of the generated output. Children are {@code null} for files.
     */
    public record Node(String name, String path, String type, long size, List<Node> children) {
    }

    /**
     * Aggregate counts shown next to a finished job.
     */
    public record Summary(int fileCount, int directoryCount, long totalBytes) {
    }

    public static Node build(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return new Node("", "", "directory", 0, List.of());
        }
        return buildNode(root, root);
    }

    private static Node buildNode(Path root, Path current) throws IOException {
        String relative = root.relativize(current).toString();
        String name = current.getFileName() == null ? "" : current.getFileName().toString();
        if (Files.isRegularFile(current)) {
            return new Node(name, relative, "file", Files.size(current), null);
        }
        List<Node> children = new ArrayList<>();
        try (Stream<Path> entries = Files.list(current)) {
            List<Path> sorted = entries.sorted(FileTrees::compareEntries).toList();
            for (Path entry : sorted) {
                children.add(buildNode(root, entry));
            }
        }
        return new Node(relative.isEmpty() ? "" : name, relative, "directory", 0, children);
    }

    /** Directories first, then files, alphabetically within each group. */
    private static int compareEntries(Path left, Path right) {
        boolean leftDir = Files.isDirectory(left);
        boolean rightDir = Files.isDirectory(right);
        if (leftDir != rightDir) {
            return leftDir ? -1 : 1;
        }
        return Comparator.<String>naturalOrder().compare(
                left.getFileName().toString(), right.getFileName().toString());
    }

    public static Summary summarize(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return new Summary(0, 0, 0);
        }
        int[] files = {0};
        int[] directories = {0};
        long[] bytes = {0};
        try (Stream<Path> paths = Files.walk(root)) {
            paths.forEach(path -> {
                if (path.equals(root)) {
                    return;
                }
                if (Files.isDirectory(path)) {
                    directories[0]++;
                    return;
                }
                files[0]++;
                try {
                    bytes[0] += Files.size(path);
                } catch (IOException ignored) {
                    // a file removed mid-walk simply does not contribute to the total
                }
            });
        }
        return new Summary(files[0], directories[0], bytes[0]);
    }

    /**
     * Resolves a client supplied relative path inside {@code root}, rejecting anything that escapes it.
     */
    public static Path resolveSafely(Path root, String relativePath) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relativePath == null ? "" : relativePath).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Path escapes the artifact directory: " + relativePath);
        }
        return resolved;
    }
}
