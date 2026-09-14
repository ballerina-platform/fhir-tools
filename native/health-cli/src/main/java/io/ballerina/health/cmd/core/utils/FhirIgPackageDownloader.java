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

package io.ballerina.health.cmd.core.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Downloads FHIR IG packages from the FHIR package registry and extracts JSON resources for codegen.
 * Mirrors the fetch/extract flow in fhir-server-go {@code internal/ig}.
 */
public final class FhirIgPackageDownloader {

    private static final String PACKAGE_PREFIX = "package/";
    private static final String VERSION_LATEST_TAG = "latest";

    private FhirIgPackageDownloader() {
    }

    public static ParsedIgSpec parseIgSpec(String igName, String igVersion) {
        if (igName == null || igName.trim().isEmpty()) {
            throw new IllegalArgumentException("IG package name cannot be empty");
        }
        String name = igName.trim();
        String version = (igVersion == null || igVersion.trim().isEmpty())
                ? VERSION_LATEST_TAG : igVersion.trim();
        return new ParsedIgSpec(name, version);
    }

    /**
     * Parses an npm-style IG reference ({@code <name>[@version]}, e.g. {@code hl7.fhir.us.core@8.0.1}) as accepted
     * by the {@code --ig} option. A missing version resolves to {@code latest}.
     */
    public static ParsedIgSpec parseIgReference(String igReference) {
        if (igReference == null || igReference.trim().isEmpty()) {
            throw new IllegalArgumentException("IG reference cannot be empty");
        }
        String trimmed = igReference.trim();
        int at = trimmed.lastIndexOf('@');
        String name = at >= 0 ? trimmed.substring(0, at) : trimmed;
        String version = at >= 0 ? trimmed.substring(at + 1) : null;
        return parseIgSpec(name, version);
    }

    /**
     * Returns {@code true} when {@code igReference} has a non-empty name component before the last {@code @}.
     */
    public static boolean isValidIgReference(String igReference) {
        if (igReference == null || igReference.trim().isEmpty()) {
            return false;
        }
        String trimmed = igReference.trim();
        int at = trimmed.lastIndexOf('@');
        String name = at >= 0 ? trimmed.substring(0, at) : trimmed;
        return !name.trim().isEmpty();
    }

    public static String fetchPackageMetadata(String registryUrl, String packageName, int timeoutSeconds)
            throws BallerinaHealthException {
        String registryBase = registryUrl.replaceAll("/+$", "");
        String metadataUrl = registryBase + "/" + packageName;
        byte[] metadataBody = httpGet(metadataUrl, timeoutSeconds);
        return new String(metadataBody, StandardCharsets.UTF_8);
    }

    /**
     * Resolves the package version to download. When version is omitted or {@code latest}, fetches registry
     * metadata and either prompts for a version (interactive) or uses {@code dist-tags.latest} (non-interactive).
     */
    public static String resolvePackageVersion(String registryUrl, String packageName, String requestedVersion,
                                               DownloadOptions options) throws BallerinaHealthException {
        if (!needsVersionResolution(requestedVersion)) {
            return requestedVersion.trim();
        }
        String metadataJson = fetchPackageMetadata(registryUrl, packageName, options.httpTimeoutSeconds());
        String latestTag = parseLatestVersionFromMetadata(metadataJson, packageName);
        List<String> availableVersions = IgVersionSelector.parseAvailableVersions(metadataJson);
        return IgVersionSelector.selectVersion(
                packageName,
                availableVersions,
                latestTag,
                options.nonInteractive(),
                options.printStream());
    }

    /**
     * Parses {@code dist-tags.latest} from FHIR package registry metadata JSON.
     */
    public static String parseLatestVersionFromMetadata(String metadataJson, String packageName)
            throws BallerinaHealthException {
        try {
            JsonObject root = JsonParser.parseString(metadataJson).getAsJsonObject();
            if (!root.has("dist-tags") || !root.get("dist-tags").isJsonObject()) {
                throw new BallerinaHealthException("Package metadata for " + packageName
                        + " does not contain dist-tags.");
            }
            JsonObject distTags = root.getAsJsonObject("dist-tags");
            if (!distTags.has(VERSION_LATEST_TAG)
                    || distTags.get(VERSION_LATEST_TAG).getAsString().trim().isEmpty()) {
                throw new BallerinaHealthException("Package metadata for " + packageName
                        + " does not contain dist-tags.latest.");
            }
            return distTags.get(VERSION_LATEST_TAG).getAsString().trim();
        } catch (BallerinaHealthException e) {
            throw e;
        } catch (Exception e) {
            throw new BallerinaHealthException("Failed to parse package metadata for " + packageName + ": "
                    + e.getMessage(), e);
        }
    }

    public static ParsedIgSpec resolveIgSpec(String igName, String igVersion, DownloadOptions options)
            throws BallerinaHealthException {
        ParsedIgSpec spec = parseIgSpec(igName, igVersion);
        String resolvedVersion = resolvePackageVersion(options.registryUrl(), spec.name(), spec.version(), options);
        return new ParsedIgSpec(spec.name(), resolvedVersion);
    }

    /**
     * True when {@code targetDirectory} already has an extracted IG whose {@code package.json} version differs
     * from {@code requestedVersion}. A directory with no readable version is assumed to already match, so a
     * plain user-supplied local spec directory (no package.json) is never treated as stale.
     */
    private static boolean isVersionMismatch(Path targetDirectory, String requestedVersion) {
        String installedVersion = SpecificationPathUtils.readInstalledPackageVersion(targetDirectory);
        return installedVersion != null && !installedVersion.equals(requestedVersion);
    }

    private static boolean needsVersionResolution(String requestedVersion) {
        return requestedVersion == null || requestedVersion.trim().isEmpty()
                || VERSION_LATEST_TAG.equalsIgnoreCase(requestedVersion.trim());
    }

    /**
     * Downloads (or reads from cache) and extracts the IG package into {@code targetDirectory}.
     */
    public static Path downloadAndExtract(Path targetDirectory, String packageName, String packageVersion,
                                         DownloadOptions options) throws BallerinaHealthException {
        return downloadAndExtract(targetDirectory, resolveIgSpec(packageName, packageVersion, options), options);
    }

    /**
     * Downloads (or reads from cache) and extracts the IG package into {@code targetDirectory}.
     */
    public static Path downloadAndExtract(Path targetDirectory, ParsedIgSpec spec, DownloadOptions options)
            throws BallerinaHealthException {
        boolean alreadyPresent = SpecificationPathUtils.containsStructureDefinitionResources(targetDirectory);
        boolean versionMismatch = alreadyPresent && isVersionMismatch(targetDirectory, spec.version());
        if (!options.forceDownload() && alreadyPresent && !versionMismatch) {
            return targetDirectory;
        }

        if (versionMismatch) {
            PrintStream out = options.printStream() != null ? options.printStream() : System.out;
            out.println(HealthCmdConstants.PrintStrings.IG_VERSION_MISMATCH_WARNING + targetDirectory
                    + " — installed version doesn't match the requested " + spec.version() + ".");
        }

        try {
            if ((options.forceDownload() || versionMismatch) && Files.exists(targetDirectory)) {
                deleteDirectoryContents(targetDirectory);
            }
            Files.createDirectories(targetDirectory);
        } catch (IOException e) {
            throw new BallerinaHealthException("Failed to prepare IG extract directory: " + targetDirectory, e);
        }

        byte[] tgzData = fetchPackageBytes(spec, options);
        extractPackage(tgzData, targetDirectory);

        if (!SpecificationPathUtils.containsStructureDefinitionResources(targetDirectory)) {
            throw new BallerinaHealthException("Downloaded IG package did not contain any StructureDefinition "
                    + "resources under " + targetDirectory);
        }
        return targetDirectory;
    }

    /**
     * Extracts a FHIR package tarball into {@code targetDirectory} (for tests and local .tgz use).
     */
    public static void extractPackage(byte[] tgzData, Path targetDirectory) throws BallerinaHealthException {
        Path normalizedTarget = targetDirectory.toAbsolutePath().normalize();
        try (InputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(tgzData));
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
            ArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                if (!entry.getName().startsWith(PACKAGE_PREFIX) || entry.isDirectory()) {
                    continue;
                }
                String relative = entry.getName().substring(PACKAGE_PREFIX.length());
                if (!relative.endsWith(".json")) {
                    continue;
                }
                Path destination = normalizedTarget.resolve(relative).normalize();
                if (!destination.startsWith(normalizedTarget)) {
                    throw new BallerinaHealthException("IG package contains an illegal entry path: "
                            + entry.getName());
                }
                Files.createDirectories(destination.getParent());
                writeEntry(tarIn, destination);
            }
        } catch (IOException e) {
            throw new BallerinaHealthException("Failed to extract IG package: " + e.getMessage(), e);
        }
    }

    private static byte[] fetchPackageBytes(ParsedIgSpec spec, DownloadOptions options)
            throws BallerinaHealthException {
        Path cachePath = resolveCachePath(options.cacheDir(), spec.name(), spec.version());
        if (cachePath != null && Files.exists(cachePath) && !options.forceDownload()) {
            try {
                return Files.readAllBytes(cachePath);
            } catch (IOException e) {
                throw new BallerinaHealthException("Failed to read cached IG package: " + cachePath, e);
            }
        }

        PrintStream out = options.printStream() != null ? options.printStream() : System.out;
        if (options.cacheDirExplicit() && cachePath != null) {
            out.println(HealthCmdConstants.PrintStrings.IG_CACHE_MISS_WARNING + cachePath
                    + ". Downloading from the registry instead.");
        }

        String registryUrl = options.registryUrl().replaceAll("/+$", "");
        String url = registryUrl + "/" + spec.name() + "/" + spec.version();
        byte[] data = httpGet(url, options.httpTimeoutSeconds());

        if (cachePath != null) {
            try {
                Files.createDirectories(cachePath.getParent());
                Files.write(cachePath, data);
                if (options.cacheDirRelativeToProject()) {
                    out.println(HealthCmdConstants.PrintStrings.IG_CACHE_GITIGNORE_WARNING
                            + options.cacheDir() + "/");
                }
            } catch (IOException ignored) {
                // best-effort cache write
            }
        }
        return data;
    }

    static byte[] httpGet(String url, int timeoutSeconds) throws BallerinaHealthException {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new BallerinaHealthException("Failed to download IG package from " + url
                        + " (HTTP " + response.statusCode() + ")");
            }
            return response.body();
        } catch (BallerinaHealthException e) {
            throw e;
        } catch (Exception e) {
            throw new BallerinaHealthException("Failed to download IG package from " + url + ": " + e.getMessage(), e);
        }
    }

    private static Path resolveCachePath(String cacheDir, String name, String version) {
        if (cacheDir == null || cacheDir.isEmpty()) {
            return null;
        }
        String safeName = name.replace('/', '_').replace('\\', '_');
        return Path.of(cacheDir).resolve(safeName + "-" + version + ".tgz");
    }

    private static void writeEntry(TarArchiveInputStream tarIn, Path destination) throws IOException {
        try (OutputStream out = Files.newOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = tarIn.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        }
    }

    private static void deleteDirectoryContents(Path directory) throws BallerinaHealthException {
        try (var paths = Files.walk(directory)) {
            paths.sorted((a, b) -> b.compareTo(a))
                    .filter(path -> !path.equals(directory))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException io) {
                throw new BallerinaHealthException("Failed to clear directory " + directory, io);
            }
            throw e;
        } catch (IOException e) {
            throw new BallerinaHealthException("Failed to clear directory " + directory, e);
        }
    }

    public static String sanitizeIgDirectoryName(String igName) {
        return igName.replace('.', '_').replace('/', '_').toLowerCase(Locale.ROOT);
    }

    public record ParsedIgSpec(String name, String version) {
        public String source() {
            return name + "@" + version;
        }
    }

    public record DownloadOptions(
            String registryUrl,
            String cacheDir,
            int httpTimeoutSeconds,
            boolean forceDownload,
            boolean nonInteractive,
            boolean cacheDirExplicit,
            boolean cacheDirRelativeToProject,
            PrintStream printStream
    ) {
        public DownloadOptions(String registryUrl, String cacheDir, int httpTimeoutSeconds, boolean forceDownload) {
            this(registryUrl, cacheDir, httpTimeoutSeconds, forceDownload, false, false, true, System.out);
        }
    }
}
