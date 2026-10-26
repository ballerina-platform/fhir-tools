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

package io.ballerina.health.service.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.health.cmd.core.config.IgRegistryConfig;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.FhirIgPackageDownloader;
import io.ballerina.health.cmd.core.utils.IgRegistryConfigLoader;
import io.ballerina.health.cmd.core.utils.IgVersionSelector;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Reads FHIR implementation guide metadata from the package registry on behalf of the UI, and pins the concrete
 * version a job should use.
 */
public class IgCatalogService {

    /** Suggestions offered in the UI. The registry is authoritative; this list only seeds the picker. */
    private static final List<CatalogEntry> SUGGESTED_IGS = List.of(
            new CatalogEntry("hl7.fhir.r4.core", "HL7 FHIR R4 Core", "r4",
                    "International base R4 resource and datatype definitions."),
            new CatalogEntry("hl7.fhir.r5.core", "HL7 FHIR R5 Core", "r5",
                    "International base R5 resource and datatype definitions."),
            new CatalogEntry("hl7.fhir.us.core", "US Core", "r4",
                    "US Core implementation guide profiles."),
            new CatalogEntry("hl7.fhir.au.base", "AU Base", "r4",
                    "Australian base implementation guide profiles."),
            new CatalogEntry("hl7.fhir.uv.sdc", "Structured Data Capture", "r4",
                    "Questionnaire and form oriented profiles."),
            new CatalogEntry("hl7.fhir.eu.base", "EU Base", "r5",
                    "European base implementation guide profiles."),
            new CatalogEntry("hl7.fhir.us.carin-bb", "CARIN Blue Button", "r4",
                    "Consumer directed payer data exchange profiles.")
    );

    private final IgRegistryConfig registryConfig;
    private final Map<String, PackageVersions> versionCache = new ConcurrentHashMap<>();
    private final Map<String, List<CatalogEntry>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> resourceTypeCache = new ConcurrentHashMap<>();

    public IgCatalogService() {
        this.registryConfig = IgRegistryConfigLoader.load();
    }

    public IgRegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public List<CatalogEntry> getSuggestedIgs() {
        return SUGGESTED_IGS;
    }

    /**
     * Returns the Ballerina package a given IG version is already published as, or {@code null} when the IG has
     * to be embedded into the generated template. Mappings are keyed by {@code <igName>@<igVersion>}, so a
     * version is required -- a mapping confirmed for one IG version must not be applied to another.
     */
    public String resolveDependentPackage(String igName, String igVersion) {
        return igName == null || igVersion == null ? null
                : registryConfig.resolveDependentPackage(igName, igVersion);
    }

    public Map<String, String> getDefaultPackages() {
        Map<String, String> defaults = new LinkedHashMap<>();
        for (String fhirVersion : List.of("r4", "r5")) {
            IgRegistryConfig.IgPackageRef ref = registryConfig.getDefaultPackage(fhirVersion);
            if (ref != null) {
                defaults.put(fhirVersion, ref.name() + "@" + ref.version());
            }
        }
        return defaults;
    }

    /**
     * Fetches the published versions of a registry package, newest first.
     */
    public PackageVersions getVersions(String packageName, String registryUrlOverride, boolean refresh)
            throws BallerinaHealthException {
        String registryUrl = registryUrl(registryUrlOverride);
        String cacheKey = registryUrl + "|" + packageName;
        if (!refresh) {
            PackageVersions cached = versionCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        String metadata;
        try {
            metadata = FhirIgPackageDownloader.fetchPackageMetadata(
                    registryUrl, packageName, registryConfig.getHttpTimeoutSeconds());
        } catch (BallerinaHealthException e) {
            throw translateNotFound(packageName, e);
        }
        List<String> versions = IgVersionSelector.parseAvailableVersions(metadata);
        String latest = FhirIgPackageDownloader.parseLatestVersionFromMetadata(metadata, packageName);
        PackageVersions result = new PackageVersions(packageName, latest, versions,
                readDescription(metadata), readFhirVersion(metadata, latest));
        versionCache.put(cacheKey, result);
        return result;
    }

    /**
     * Pins the exact version a job will use so that the job record, the cache directory layout and the UI all
     * agree even when the caller asked for {@code latest}.
     */
    public String pinVersion(String packageName, String requestedVersion, String registryUrlOverride)
            throws BallerinaHealthException {
        if (requestedVersion != null && !requestedVersion.isBlank()
                && !"latest".equalsIgnoreCase(requestedVersion.trim())) {
            return requestedVersion.trim();
        }
        FhirIgPackageDownloader.DownloadOptions options = new FhirIgPackageDownloader.DownloadOptions(
                registryUrl(registryUrlOverride),
                registryConfig.getCacheDir(),
                registryConfig.getHttpTimeoutSeconds(),
                false,
                true,
                false,
                true,
                discardingStream());
        try {
            return FhirIgPackageDownloader.resolveIgSpec(packageName, requestedVersion, options).version();
        } catch (BallerinaHealthException e) {
            throw translateNotFound(packageName, e);
        }
    }

    /**
     * The downloader reports every non-200 registry response as one exception type, so the status has to be read
     * back out of the message to tell "no such package" apart from a registry outage.
     */
    private static BallerinaHealthException translateNotFound(String packageName, BallerinaHealthException cause) {
        String message = cause.getMessage();
        if (message != null && message.contains("(HTTP 404)")) {
            return new PackageNotFoundException(packageName, cause);
        }
        return cause;
    }

    /**
     * The registry has no package under the requested id.
     */
    public static class PackageNotFoundException extends BallerinaHealthException {
        public PackageNotFoundException(String packageName, Throwable cause) {
            super("Implementation guide '" + packageName + "' was not found on the FHIR package registry.", cause);
        }
    }

    private String registryUrl(String override) {
        return override != null && !override.isBlank() ? override.trim() : registryConfig.getRegistryUrl();
    }

    /** Swallows the interactive prompts the downloader would otherwise print to the server console. */
    private static PrintStream discardingStream() {
        return new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
    }

    private static String readDescription(String metadataJson) {
        try {
            JsonObject root = JsonParser.parseString(metadataJson).getAsJsonObject();
            return root.has("description") ? root.get("description").getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readFhirVersion(String metadataJson, String version) {
        try {
            JsonObject root = JsonParser.parseString(metadataJson).getAsJsonObject();
            if (!root.has("versions") || version == null) {
                return null;
            }
            JsonObject versions = root.getAsJsonObject("versions");
            if (!versions.has(version)) {
                return null;
            }
            JsonObject entry = versions.getAsJsonObject(version);
            if (entry.has("fhirVersion")) {
                return entry.get("fhirVersion").getAsString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Searches the package registry for implementation guides whose name matches {@code query}.
     *
     * <p>Uses the registry's catalog endpoint ({@code /catalog?op=find&name=...}), which returns every published
     * package rather than the handful {@link #getSuggestedIgs()} seeds the picker with. An empty query lists the
     * whole catalog, so callers should require a couple of characters before searching.
     */
    public List<CatalogEntry> search(String query, String registryUrlOverride) throws BallerinaHealthException {
        String registryUrl = registryUrl(registryUrlOverride);
        String term = query == null ? "" : query.trim();
        String cacheKey = registryUrl + "|" + term.toLowerCase();
        List<CatalogEntry> cached = searchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String url = trimTrailingSlash(registryUrl) + "/catalog?op=find&name="
                + URLEncoder.encode(term, StandardCharsets.UTF_8);
        String body = httpGet(url);
        List<CatalogEntry> results = new ArrayList<>();
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed.isJsonArray()) {
                JsonArray array = parsed.getAsJsonArray();
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject entry = element.getAsJsonObject();
                    String name = readString(entry, "Name");
                    if (name == null || name.isBlank()) {
                        continue;
                    }
                    // The catalog reports FHIR versions as R4/R4B/R5/STU3; the tool keys everything off the
                    // lower-case short form, so normalise here rather than in every caller.
                    String fhirVersion = readString(entry, "FhirVersion");
                    results.add(new CatalogEntry(name, name,
                            fhirVersion == null ? null : fhirVersion.toLowerCase(),
                            readString(entry, "Description")));
                }
            }
        } catch (Exception e) {
            throw new BallerinaHealthException("Unable to read the registry catalog response", e);
        }
        results.sort((a, b) -> rank(a.name(), term) - rank(b.name(), term));
        searchCache.put(cacheKey, results);
        return results;
    }

    /**
     * Lists the FHIR resource types an implementation guide profiles, i.e. the set of APIs a generated facade
     * would expose. Requires the package's definitions on disk, so the IG is downloaded and extracted into
     * {@code cacheDir} on first use and read from there afterwards.
     */
    public List<String> listResourceTypes(String packageName, String version, String registryUrlOverride,
                                          Path cacheDir) throws BallerinaHealthException {
        String cacheKey = packageName + "@" + version;
        List<String> cached = resourceTypeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        FhirIgPackageDownloader.DownloadOptions options = new FhirIgPackageDownloader.DownloadOptions(
                registryUrl(registryUrlOverride),
                registryConfig.getCacheDir(),
                registryConfig.getHttpTimeoutSeconds(),
                false,
                true,
                false,
                true,
                discardingStream());
        Path specDir = FhirIgPackageDownloader.downloadAndExtract(cacheDir, packageName, version, options);
        Set<String> resourceTypes = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(specDir)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                String type = profiledResourceType(file);
                if (type != null) {
                    resourceTypes.add(type);
                }
            }
        } catch (Exception e) {
            throw new BallerinaHealthException("Unable to read the definitions of " + cacheKey, e);
        }
        List<String> result = List.copyOf(resourceTypes);
        resourceTypeCache.put(cacheKey, result);
        return result;
    }

    /**
     * The resource type a StructureDefinition profiles, or null when the file is not a resource profile (a
     * datatype or extension definition, a ValueSet, an example, ...). Only constraint-derived resource profiles
     * become services in a generated facade, which is what the API picker offers.
     */
    private static String profiledResourceType(Path file) {
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file));
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject json = parsed.getAsJsonObject();
            if (!"StructureDefinition".equals(readString(json, "resourceType"))) {
                return null;
            }
            if (!"resource".equals(readString(json, "kind"))) {
                return null;
            }
            if (!"constraint".equals(readString(json, "derivation"))) {
                return null;
            }
            String type = readString(json, "type");
            return type == null || type.isBlank() ? null : type;
        } catch (Exception e) {
            // A malformed or unreadable definition should not fail the whole listing.
            return null;
        }
    }

    /** Exact match first, then prefix matches, then the rest, so the obvious package is at the top. */
    private static int rank(String name, String term) {
        if (term.isEmpty()) {
            return 2;
        }
        String lowerName = name.toLowerCase();
        String lowerTerm = term.toLowerCase();
        if (lowerName.equals(lowerTerm)) {
            return 0;
        }
        return lowerName.startsWith(lowerTerm) ? 1 : 2;
    }

    private static String readString(JsonObject json, String member) {
        if (json == null || !json.has(member) || json.get(member).isJsonNull()) {
            return null;
        }
        try {
            return json.get(member).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String httpGet(String url) throws BallerinaHealthException {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(registryConfig.getHttpTimeoutSeconds()))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(registryConfig.getHttpTimeoutSeconds()))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BallerinaHealthException(
                        "Registry responded with HTTP " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (BallerinaHealthException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BallerinaHealthException("Interrupted while querying the registry", e);
        } catch (Exception e) {
            throw new BallerinaHealthException("Unable to reach the registry at " + url, e);
        }
    }

    /**
     * An implementation guide suggested in the UI picker.
     */
    public record CatalogEntry(String name, String title, String fhirVersion, String description) {
    }

    /**
     * Published versions of a registry package.
     */
    public record PackageVersions(String name, String latest, List<String> versions, String description,
                                  String fhirVersion) {
        public PackageVersions {
            versions = versions == null ? new ArrayList<>() : List.copyOf(versions);
        }
    }
}
