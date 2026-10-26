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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip helpers for artifact downloads and specification uploads.
 */
public final class Zips {

    private Zips() {
    }

    /**
     * Streams {@code sourceDir} into {@code target} as a zip archive, nesting everything under
     * {@code entryPrefix}.
     */
    public static void zipDirectory(Path sourceDir, String entryPrefix, OutputStream target) throws IOException {
        Path root = sourceDir.toAbsolutePath().normalize();
        try (ZipOutputStream zip = new ZipOutputStream(target)) {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String entryName = entryPrefix + "/" + root.relativize(file).toString().replace('\\', '/');
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zip);
                    zip.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Extracts a zip archive into {@code targetDir}, skipping entries that would escape it.
     *
     * @return number of files written
     */
    public static int unzip(InputStream source, Path targetDir) throws IOException {
        Path root = targetDir.toAbsolutePath().normalize();
        Files.createDirectories(root);
        int written = 0;
        try (ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path resolved = root.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(root)) {
                    // guard against zip slip
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                    continue;
                }
                Files.createDirectories(resolved.getParent());
                try (OutputStream out = Files.newOutputStream(resolved)) {
                    zip.transferTo(out);
                }
                written++;
            }
        }
        return written;
    }
}
