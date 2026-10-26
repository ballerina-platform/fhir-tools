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
import io.ballerina.health.service.ServiceConfig;
import io.ballerina.health.service.util.PathUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Gives each opened FHIR package an editable copy of its definitions, so the UI can show what an implementation
 * guide actually declares and let people change it before generating a facade from it.
 *
 * <p>The copy matters: the pristine extraction under {@code spec-cache/} is shared by every job that uses the
 * package, so edits must never land there. A workspace is a plain directory of definition JSON files, which is
 * exactly the shape the generator already accepts as a local specification path, so generating from an edited
 * package needs no special case downstream.
 */
public class PackageWorkspaceService {

    /** How stale a workspace's last-used time may get before it is rewritten. */
    private static final Duration TOUCH_INTERVAL = Duration.ofHours(1);

    private static final String METADATA_FILE = ".package.json";

    private final ServiceConfig config;
    private final IgCatalogService igCatalog;
    /**
     * Built lazily: it loads the ~12MB FHIR R4 core package to resolve base types, which takes a few seconds,
     * and is only needed once someone actually opens a definition's table view.
     */
    private volatile StructureDefinitionTableRenderer tableRenderer;
    private volatile String tableRendererError;
    private volatile R5TableRenderer r5TableRenderer;
    private volatile String r5TableRendererError;
    /** url -> file of every definition in a core package, built once per FHIR version. */
    private final Map<String, Map<String, Path>> coreIndexes = new ConcurrentHashMap<>();
    /** Expanding a definition reads every datatype it touches, so the result is kept until the file changes. */
    private final Map<String, ExpansionCacheEntry> expansions = new ConcurrentHashMap<>();
    /** Resource type names per FHIR version, so the core package is only scanned for them once. */
    private final Map<String, List<String>> coreResourceTypes = new ConcurrentHashMap<>();
    /** Datatype definitions, shared across expansions: the same handful are read over and over. */
    private final Map<String, Map<String, JsonObject>> resolvedDefinitions = new ConcurrentHashMap<>();

    public PackageWorkspaceService(ServiceConfig config, IgCatalogService igCatalog) {
        this.config = config;
        this.igCatalog = igCatalog;
    }

    /**
     * Opens a registry package for exploration, downloading and extracting it if needed. Re-opening the same
     * package and version returns the existing workspace with any edits intact, rather than discarding them.
     */
    public Workspace open(String packageName, String requestedVersion, String registryUrlOverride, String owner)
            throws BallerinaHealthException, IOException {
        String version = requestedVersion == null || requestedVersion.isBlank()
                || "latest".equalsIgnoreCase(requestedVersion)
                ? igCatalog.pinVersion(packageName, requestedVersion, registryUrlOverride)
                : requestedVersion.trim();

        String workspaceId = workspaceIdFor(packageName, version, owner);
        Path workspaceDir = workspaceDir(workspaceId);
        if (!Files.isDirectory(workspaceDir) || isEmpty(workspaceDir)) {
            Path pristine = pristineDir(packageName, version, registryUrlOverride);
            copyDefinitions(pristine, workspaceDir);
            writeMetadata(workspaceId, packageName, version, registryUrlOverride, owner);
        }
        touch(workspaceId);
        return describe(workspaceId);
    }

    /**
     * The id of one browser's working copy of a package.
     *
     * <p>Keyed by the package alone, a working copy is shared by everyone pointed at the service: two people
     * editing the same guide overwrite each other, and the second is shown work that is not theirs. Folding in
     * who is asking gives each their own. The owner is hashed rather than spelled out, so a directory listing
     * on the server does not enumerate its users.
     */
    private String workspaceIdFor(String packageName, String version, String owner) {
        String base = packageName + "-" + version;
        return owner == null || owner.isBlank()
                ? PathUtils.slug(base, "ig")
                : PathUtils.slug(base + "-" + shortHash(owner), "ig");
    }

    /** Eight hex characters of SHA-256: enough that two browsers will not collide, short enough to read. */
    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }

    /**
     * Refuses a workspace that belongs to someone else.
     *
     * <p>Reported as "no such package" rather than "not yours": which packages another person is working on is
     * not something worth confirming to someone guessing at ids.
     *
     * <p>Workspaces created before ownership existed carry no owner. Those stay readable by anyone, which is
     * exactly what they already were -- refusing them would break work that predates this without making
     * anything safer.
     */
    /** Whether a workspace is this caller's, or is one of the unowned ones everybody shares. */
    public boolean isVisibleTo(String workspaceId, String owner) {
        try {
            String recorded = readMetadata(workspaceId).owner();
            return recorded == null || recorded.isBlank() || recorded.equals(owner);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Reclaims working copies nobody has touched for a while.
     *
     * <p>A copy per browser per package is the price of not sharing them, and browsers are not tidy: someone
     * who opens a guide once leaves several megabytes behind for good. Anything untouched past the cut-off goes,
     * along with its metadata. Nothing is lost that cannot be rebuilt -- the package comes from the registry
     * again, and the edits are in whichever browser made them.
     *
     * @return how many were reclaimed
     */
    public int sweepAbandoned(Duration keepFor) {
        Path root = config.getSpecsRoot();
        if (!Files.isDirectory(root) || keepFor == null || keepFor.isZero() || keepFor.isNegative()) {
            return 0;
        }
        Instant cutOff = Instant.now().minus(keepFor);
        int removed = 0;
        try (Stream<Path> files = Files.list(root)) {
            List<Path> metadata = files.filter(f -> f.getFileName().toString().endsWith(METADATA_FILE))
                    .toList();
            for (Path file : metadata) {
                String name = file.getFileName().toString();
                String workspaceId = name.substring(0, name.length() - METADATA_FILE.length());
                Metadata entry = readMetadata(workspaceId);
                // Only owned copies are swept: the shared, unowned ones predate this and are not ours to bin.
                if (entry.owner() == null || entry.owner().isBlank() || entry.touchedAt() == null) {
                    continue;
                }
                Instant touched;
                try {
                    touched = Instant.parse(entry.touchedAt());
                } catch (Exception e) {
                    continue;
                }
                if (touched.isBefore(cutOff)) {
                    PathUtils.deleteRecursively(workspaceDir(workspaceId));
                    Files.deleteIfExists(file);
                    removed++;
                }
            }
        } catch (IOException e) {
            // A sweep that cannot run leaves disk behind, which is not worth taking the service down for.
        }
        return removed;
    }

    public void requireOwner(String workspaceId, String owner) throws IOException {
        String recorded = readMetadata(workspaceId).owner();
        if (recorded == null || recorded.isBlank()) {
            return;
        }
        if (!recorded.equals(owner)) {
            throw new IllegalArgumentException("No such package: " + workspaceId);
        }
    }

    /** Restores every definition from the pristine extraction, discarding edits. */
    public Workspace reset(String workspaceId) throws BallerinaHealthException, IOException {
        Metadata metadata = readMetadata(workspaceId);
        if (metadata.version() == null || metadata.version().isBlank()) {
            // An uploaded package has no registry copy to restore from. Checked before anything is deleted:
            // wiping the workspace first and only then discovering there is nothing to re-fetch would destroy
            // the only copy of it.
            throw new IllegalArgumentException(
                    "This package was uploaded, so there is no published version to restore. Re-upload the "
                            + "archive to discard the changes.");
        }
        Path workspaceDir = workspaceDir(workspaceId);
        PathUtils.deleteRecursively(workspaceDir);
        Path pristine = pristineDir(metadata.packageName(), metadata.version(), metadata.registryUrl());
        copyDefinitions(pristine, workspaceDir);
        writeMetadata(workspaceId, metadata.packageName(), metadata.version(), metadata.registryUrl(),
                metadata.owner());
        return describe(workspaceId);
    }

    public Workspace describe(String workspaceId) throws IOException {
        Metadata metadata = readMetadata(workspaceId);
        List<Definition> definitions = listDefinitions(workspaceId);
        JsonObject manifest = packageManifest(workspaceId);
        return new Workspace(workspaceId, metadata.packageName(), metadata.version(), metadata.registryUrl(),
                fhirVersionOf(definitions), definitions.size(), defaultProjectName(metadata),
                metadata.editedCount(), readString(manifest, "title"), readString(manifest, "description"),
                readString(manifest, "author"), readString(manifest, "canonical"));
    }

    /**
     * The package's own manifest, which carries what the guide says about itself.
     *
     * <p>Read from the package rather than the registry so it describes the version actually open, and so an
     * uploaded archive is described too.
     */
    private JsonObject packageManifest(String workspaceId) {
        Path manifest = workspaceDir(workspaceId).resolve("package.json");
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(manifest, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Every StructureDefinition in the workspace, summarised for the browser list. Other resource types
     * (ValueSet, CodeSystem, the ImplementationGuide itself) are part of the package but are not what a facade
     * is shaped by, so they are not listed.
     */
    public List<Definition> listDefinitions(String workspaceId) throws IOException {
        Path dir = workspaceDir(workspaceId);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Definition> definitions = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                Definition definition = summarise(file);
                if (definition != null) {
                    definitions.add(definition);
                }
            }
        }
        definitions.sort(Comparator.comparing(Definition::resourceType).thenComparing(Definition::name));
        return definitions;
    }

    /** Where an extension may be used, read from its context; empty for anything that is not an extension. */
    private static List<String> contexts(JsonObject json) {
        List<String> expressions = new ArrayList<>();
        if (json == null || !json.has("context") || !json.get("context").isJsonArray()) {
            return expressions;
        }
        for (JsonElement entry : json.getAsJsonArray("context")) {
            if (entry.isJsonObject()) {
                String expression = readString(entry.getAsJsonObject(), "expression");
                if (expression != null) {
                    expressions.add(expression);
                }
            }
        }
        return expressions;
    }

    /**
     * Adds a new extension to the package.
     *
     * <p>Written into the workspace like any other definition, so it shows up in the browser, renders in the
     * element tables, and is picked up by generation. It is validated against the base Extension definition the
     * same way an edit is, which is what catches a value type or cardinality that is not a legal constraint.
     */
    public Definition createExtension(String workspaceId, String id, String name, String title,
                                      String description, List<String> contexts, List<String> valueTypes,
                                      boolean required, boolean repeats) throws IOException {
        JsonObject extension = ExtensionAuthor.build(id, name, title, description, canonicalBase(workspaceId),
                contexts, valueTypes, required, repeats);
        String fileName = "StructureDefinition-" + ExtensionAuthor.sanitiseId(id) + ".json";
        Path file = workspaceDir(workspaceId).resolve(fileName);
        if (Files.exists(file)) {
            throw new IllegalArgumentException("A definition called '" + fileName + "' already exists.");
        }
        JsonObject base = resolveBaseDefinition(workspaceId, extension);
        List<ProfileRuleValidator.Finding> findings =
                ProfileRuleValidator.validate(extension, extension, base);
        if (findings.stream().anyMatch(ProfileRuleValidator.Finding::isError)) {
            throw new ProfileRuleViolation(findings);
        }
        // Published extensions ship a snapshot and the generator reads that rather than the differential, so
        // without one the extension generates as a typeless record with no value element.
        regenerateSnapshot(workspaceId, extension, base);
        writeAtomically(file, extension.toString());
        markEdited(workspaceId, fileName);
        return summarise(file);
    }

    /**
     * Creates a profile of a core resource, or of another profile already in the package.
     *
     * <p>It starts constrained in no way at all -- the element table is where constraints are made -- but it is
     * given a snapshot straight away, because the tables and the generator both read that rather than the
     * differential.
     */
    public Definition createProfile(String workspaceId, String id, String title, String description,
                                    String baseUrl) throws IOException {
        JsonObject baseDefinition = definitionByUrl(workspaceId, baseUrl);
        if (baseDefinition == null) {
            throw new IllegalArgumentException("Nothing in this package or the FHIR core defines " + baseUrl);
        }
        String resourceType = readString(baseDefinition, "type");
        JsonObject profile = ProfileAuthor.build(id, title, description, canonicalBase(workspaceId), baseUrl,
                resourceType, readString(baseDefinition, "fhirVersion"));

        String fileName = "StructureDefinition-" + ExtensionAuthor.sanitiseId(id) + ".json";
        Path file = workspaceDir(workspaceId).resolve(fileName);
        if (Files.exists(file)) {
            throw new IllegalArgumentException("A definition called '" + fileName + "' already exists.");
        }
        List<ProfileRuleValidator.Finding> findings =
                ProfileRuleValidator.validate(profile, profile, baseDefinition);
        if (findings.stream().anyMatch(ProfileRuleValidator.Finding::isError)) {
            throw new ProfileRuleViolation(findings);
        }
        regenerateSnapshot(workspaceId, profile, baseDefinition);
        if (!profile.has("snapshot") && baseDefinition.has("snapshot")) {
            // Generation declines some bases outright. Inheriting the base's snapshot is honest for a profile
            // that constrains nothing yet, and leaves the tables with something real to draw.
            profile.add("snapshot", JsonParser.parseString(
                    baseDefinition.getAsJsonObject("snapshot").toString()).getAsJsonObject());
        }
        writeAtomically(file, profile.toString());
        markEdited(workspaceId, fileName);
        return summarise(file);
    }

    /** Everything a new profile could be based on: this package's own resources, then the FHIR core's. */
    public List<Map<String, Object>> baseProfiles(String workspaceId) throws IOException {
        List<Map<String, Object>> options = new ArrayList<>();
        for (Definition definition : listDefinitions(workspaceId)) {
            if ("resource".equals(definition.kind()) && definition.url() != null) {
                options.add(Map.of("url", definition.url(),
                        "label", definition.title() == null ? definition.name() : definition.title(),
                        "resourceType", String.valueOf(definition.resourceType()),
                        "source", "package"));
            }
        }
        Workspace workspace = describe(workspaceId);
        String fhirVersion = "r5".equalsIgnoreCase(String.valueOf(workspace.fhirVersion())) ? "r5" : "r4";
        for (String type : coreResourceTypes(fhirVersion)) {
            options.add(Map.of("url", "http://hl7.org/fhir/StructureDefinition/" + type,
                    "label", type, "resourceType", type,
                    "source", fhirVersion.toUpperCase(Locale.ROOT)));
        }
        return options;
    }

    /**
     * The resource types the core package defines, worked out once per FHIR version.
     *
     * <p>The core index maps canonical URLs to files; whether one is a resource rather than a datatype is only
     * in the file. Reading six hundred of them is worth doing once and remembering, not on every request.
     */
    private List<String> coreResourceTypes(String fhirVersion) {
        List<String> cached = coreResourceTypes.get(fhirVersion);
        if (cached != null) {
            return cached;
        }
        Set<String> types = new java.util.TreeSet<>();
        try {
            for (Map.Entry<String, Path> entry : coreIndex(fhirVersion).entrySet()) {
                String url = entry.getKey();
                if (!url.startsWith("http://hl7.org/fhir/StructureDefinition/")) {
                    continue;
                }
                String tail = url.substring(url.lastIndexOf('/') + 1);
                if (tail.isEmpty() || !Character.isUpperCase(tail.charAt(0)) || tail.contains("-")) {
                    continue;
                }
                JsonObject definition = JsonParser
                        .parseString(Files.readString(entry.getValue(), StandardCharsets.UTF_8))
                        .getAsJsonObject();
                if ("resource".equals(readString(definition, "kind"))
                        && !Boolean.TRUE.equals(bool(definition, "abstract"))) {
                    types.add(tail);
                }
            }
        } catch (Exception e) {
            // A core package that cannot be read leaves only the package's own resources to choose from.
        }
        List<String> result = List.copyOf(types);
        coreResourceTypes.put(fhirVersion, result);
        return result;
    }

    private static Boolean bool(JsonObject json, String member) {
        if (json == null || !json.has(member) || json.get(member).isJsonNull()) {
            return null;
        }
        try {
            return json.get(member).getAsBoolean();
        } catch (Exception e) {
            return null;
        }
    }

    /** A definition by canonical URL, from this package first and the FHIR core second. */
    private JsonObject definitionByUrl(String workspaceId, String url) throws IOException {
        if (url == null || url.isBlank()) {
            return null;
        }
        Path inPackage = findByUrl(workspaceDir(workspaceId), url);
        if (inPackage != null) {
            return JsonParser.parseString(Files.readString(inPackage, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
        Workspace workspace = describe(workspaceId);
        String fhirVersion = "r5".equalsIgnoreCase(String.valueOf(workspace.fhirVersion())) ? "r5" : "r4";
        Path inCore = coreIndex(fhirVersion).get(url);
        return inCore == null ? null
                : JsonParser.parseString(Files.readString(inCore, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /**
     * The canonical prefix the package's own definitions use, so a new extension's URL sits alongside them
     * rather than under an unrelated host.
     */
    private String canonicalBase(String workspaceId) throws IOException {
        for (Definition definition : listDefinitions(workspaceId)) {
            String url = definition.url();
            if (url != null && url.contains("/")) {
                return url.substring(0, url.lastIndexOf('/'));
            }
        }
        return "http://example.org/fhir/StructureDefinition";
    }

    /** The raw JSON of one definition, as stored, so the editor round-trips exactly what is on disk. */
    public String readDefinition(String workspaceId, String fileName) throws IOException {
        return Files.readString(definitionFile(workspaceId, fileName), StandardCharsets.UTF_8);
    }

    /**
     * Replaces one definition with edited JSON. The content has to parse and still be a StructureDefinition:
     * saving something else would leave the package in a state the generator cannot read, and the failure would
     * only surface much later as a confusing generation error.
     */
    public WriteResult writeDefinition(String workspaceId, String fileName, String content) throws IOException {
        JsonObject json;
        try {
            JsonElement parsed = JsonParser.parseString(content);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("A StructureDefinition must be a JSON object.");
            }
            json = parsed.getAsJsonObject();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("This is not valid JSON: " + e.getMessage());
        }
        if (!"StructureDefinition".equals(readString(json, "resourceType"))) {
            throw new IllegalArgumentException("The edited resource must keep resourceType \"StructureDefinition\".");
        }
        Path file = definitionFile(workspaceId, fileName);
        JsonObject original = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
        List<ProfileRuleValidator.Finding> findings =
                ProfileRuleValidator.validate(json, original, resolveBaseDefinition(workspaceId, json));
        if (findings.stream().anyMatch(ProfileRuleValidator.Finding::isError)) {
            // Refused rather than saved-with-warnings: a profile that is not a legal derivation of its base
            // would generate types that claim a conformance they do not have.
            throw new ProfileRuleViolation(findings);
        }
        writeAtomically(file, content);
        markEdited(workspaceId, fileName);
        return new WriteResult(summarise(file), findings);
    }

    /**
     * The definition a profile derives from: another profile in the same package when it builds on one, and
     * otherwise the resource or datatype in the FHIR core package.
     */
    private JsonObject resolveBaseDefinition(String workspaceId, JsonObject profile) {
        String baseUrl = profile.has("baseDefinition") && !profile.get("baseDefinition").isJsonNull()
                ? profile.get("baseDefinition").getAsString() : null;
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        try {
            Path inPackage = findByUrl(workspaceDir(workspaceId), baseUrl);
            if (inPackage != null) {
                return JsonParser.parseString(Files.readString(inPackage, StandardCharsets.UTF_8))
                        .getAsJsonObject();
            }
            Workspace workspace = describe(workspaceId);
            String fhirVersion = "r5".equalsIgnoreCase(workspace.fhirVersion()) ? "r5" : "r4";
            Path inCore = coreIndex(fhirVersion).get(baseUrl);
            if (inCore != null) {
                return JsonParser.parseString(Files.readString(inCore, StandardCharsets.UTF_8)).getAsJsonObject();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private Path findByUrl(Path directory, String url) throws IOException {
        if (!Files.isDirectory(directory)) {
            return null;
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                Definition summary = summarise(file);
                if (summary != null && url.equals(summary.url())) {
                    return file;
                }
            }
        }
        return null;
    }

    /** Indexed once: a core package holds thousands of files and every save would otherwise rescan them. */
    private Map<String, Path> coreIndex(String fhirVersion) {
        return coreIndexes.computeIfAbsent(fhirVersion, version -> {
            Map<String, Path> index = new LinkedHashMap<>();
            try {
                IgRegistryConfig.IgPackageRef core = igCatalog.getRegistryConfig().getDefaultPackage(version);
                String name = core == null
                        ? ("r5".equals(version) ? "hl7.fhir.r5.core" : "hl7.fhir.r4.core") : core.name();
                String pinned = core == null ? ("r5".equals(version) ? "5.0.0" : "4.0.1") : core.version();
                Path cacheDir = config.getWorkspaceRoot().resolve("spec-cache")
                        .resolve(PathUtils.slug(name + "-" + pinned, "ig"));
                Files.createDirectories(cacheDir);
                igCatalog.listResourceTypes(name, pinned, null, cacheDir);
                Path specRoot = cacheDir.resolve("spec");
                Path definitions = cacheDir;
                if (Files.isDirectory(specRoot)) {
                    try (Stream<Path> children = Files.list(specRoot)) {
                        definitions = children.filter(Files::isDirectory).findFirst().orElse(cacheDir);
                    }
                }
                try (Stream<Path> files = Files.walk(definitions)) {
                    for (Path file : files.filter(Files::isRegularFile)
                            .filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                        Definition summary = summarise(file);
                        if (summary != null && summary.url() != null) {
                            index.putIfAbsent(summary.url(), file);
                        }
                    }
                }
            } catch (Exception e) {
                // An unavailable core package means the base cannot be resolved; the validator downgrades to a
                // warning rather than blocking every save.
                return index;
            }
            return index;
        });
    }

    /** Raised when an edit would break the rules FHIR puts on profiling. */
    public static class ProfileRuleViolation extends RuntimeException {
        private final transient List<ProfileRuleValidator.Finding> findings;

        public ProfileRuleViolation(List<ProfileRuleValidator.Finding> findings) {
            super("The edit does not follow FHIR profiling rules.");
            this.findings = findings;
        }

        public List<ProfileRuleValidator.Finding> getFindings() {
            return findings;
        }
    }

    /** A saved definition plus any advisory findings raised while checking it. */
    public record WriteResult(Definition definition, List<ProfileRuleValidator.Finding> findings) {
    }

    /**
     * How many levels of datatype to bring inline. Two reaches the common case -- a datatype's own children and
     * the datatypes those carry, such as Patient.name.period.start -- without the row count running away.
     */
    private static final int DATATYPE_EXPANSION_PASSES = 2;

    /**
     * Finds a StructureDefinition by canonical URL, preferring the package's own.
     *
     * <p>The IG's copy comes first so a datatype it profiles expands from that profile rather than from the
     * core datatype it derives from.
     */
    private Function<String, JsonObject> definitionResolver(String workspaceId, String fhirVersion) {
        Map<String, JsonObject> cache =
                resolvedDefinitions.computeIfAbsent(workspaceId + "|" + fhirVersion, key -> new ConcurrentHashMap<>());
        return url -> {
            if (url == null || url.isBlank()) {
                return null;
            }
            if (cache.containsKey(url)) {
                return cache.get(url);
            }
            JsonObject found = null;
            try {
                Path inPackage = findByUrl(workspaceDir(workspaceId), url);
                Path file = inPackage != null ? inPackage : coreIndex(fhirVersion).get(url);
                if (file != null) {
                    JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                    found = parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
                }
            } catch (Exception e) {
                found = null;
            }
            if (found != null) {
                // Only hits are cached: a miss may just mean a definition that has not been authored yet.
                cache.put(url, found);
            }
            return found;
        };
    }

    /** The definition as shown: the stored profile with its complex datatypes brought inline. */
    private JsonObject expandedDefinition(String workspaceId, String fileName) throws IOException {
        Path file = definitionFile(workspaceId, fileName);
        long modified = Files.getLastModifiedTime(file).toMillis();
        String key = workspaceId + "|" + fileName;
        ExpansionCacheEntry cached = expansions.get(key);
        if (cached != null && cached.modified() == modified) {
            return cached.definition();
        }
        JsonElement parsed = JsonParser.parseString(readDefinition(workspaceId, fileName));
        if (!parsed.isJsonObject()) {
            throw new IOException("The definition '" + fileName + "' is not readable as a StructureDefinition.");
        }
        JsonObject definition = parsed.getAsJsonObject();
        String fhirVersion = "r5".equalsIgnoreCase(String.valueOf(describe(workspaceId).fhirVersion()))
                ? "r5" : "r4";
        JsonObject expanded = DatatypeExpander.expand(definition, definitionResolver(workspaceId, fhirVersion),
                DATATYPE_EXPANSION_PASSES);
        expansions.put(key, new ExpansionCacheEntry(modified, expanded));
        return expanded;
    }

    /** An expanded definition and the file timestamp it was built from. */
    private record ExpansionCacheEntry(long modified, JsonObject definition) {
    }

    /** Every element of a definition with the bounds its base imposes, for the structured editor. */
    public List<Map<String, Object>> describeElements(String workspaceId, String fileName) throws IOException {
        JsonObject profile = expandedDefinition(workspaceId, fileName);
        return ProfileEditor.describeElements(profile, resolveBaseDefinition(workspaceId, profile));
    }

    /**
     * Applies constraint changes to one element and saves the definition.
     *
     * <p>The change goes into the differential, the profile's own statement of what it constrains, and the
     * snapshot is then regenerated from base plus differential so the element tables and the generator see the
     * edit. It is validated exactly like a hand-edited save, so the bounded form in the UI is a convenience
     * rather than the only thing standing between a user and an illegal profile.
     */
    public WriteResult applyElementChanges(String workspaceId, String fileName, String path, String sliceName,
                                           JsonObject changes) throws IOException {
        String raw = readDefinition(workspaceId, fileName);
        JsonObject original = JsonParser.parseString(raw).getAsJsonObject();
        JsonObject edited = JsonParser.parseString(raw).getAsJsonObject();
        ProfileEditor.applyChanges(edited, path, sliceName, changes);

        JsonObject base = resolveBaseDefinition(workspaceId, edited);
        List<ProfileRuleValidator.Finding> findings = ProfileRuleValidator.validate(edited, original, base);
        if (findings.stream().anyMatch(ProfileRuleValidator.Finding::isError)) {
            throw new ProfileRuleViolation(findings);
        }
        regenerateSnapshot(workspaceId, edited, base);

        Path file = definitionFile(workspaceId, fileName);
        writeAtomically(file, edited.toString());
        markEdited(workspaceId, fileName);
        return new WriteResult(summarise(file), findings);
    }

    /**
     * Carries into the snapshot anything the differential constrains that the generator left out.
     *
     * <p>HL7's generator drops an element it cannot match against the base, and for a constraint on a
     * primitive's extension there is nothing to match: a resource snapshot never expands
     * {@code Patient.gender.extension}, and seeding the base with that element is not enough for it either. The
     * result is a profile whose differential declares an extension its snapshot does not, and the snapshot is
     * what both the element tables and the code generator read, so the extension would simply not exist as far
     * as either is concerned.
     *
     * <p>Only elements the generator omitted are added, positioned after the element they belong to, so
     * everything it did resolve is left exactly as it produced it.
     */
    private static void reconcileSnapshot(JsonObject profile) {
        if (!profile.has("snapshot") || !profile.getAsJsonObject("snapshot").has("element")
                || !profile.has("differential")) {
            return;
        }
        JsonArray snapshot = profile.getAsJsonObject("snapshot").getAsJsonArray("element");
        Set<String> present = new LinkedHashSet<>();
        for (JsonElement entry : snapshot) {
            if (entry.isJsonObject()) {
                present.add(identity(entry.getAsJsonObject()));
            }
        }

        JsonArray missing = new JsonArray();
        for (JsonElement entry : profile.getAsJsonObject("differential").getAsJsonArray("element")) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject element = entry.getAsJsonObject();
            String path = readString(element, "path");
            // Only extension points are reconciled. Anything else the generator omitted, it omitted for a
            // reason this code is not in a position to second-guess.
            if (path != null && path.endsWith(".extension") && !present.contains(identity(element))) {
                missing.add(asSnapshotElement(element, path));
            }
        }
        if (missing.isEmpty()) {
            return;
        }

        JsonArray rebuilt = new JsonArray();
        Set<String> placed = new LinkedHashSet<>();
        for (JsonElement entry : snapshot) {
            rebuilt.add(entry);
            String path = entry.isJsonObject() ? readString(entry.getAsJsonObject(), "path") : null;
            if (path == null) {
                continue;
            }
            for (JsonElement candidate : missing) {
                JsonObject element = candidate.getAsJsonObject();
                String identity = identity(element);
                // The un-sliced element comes first, then its slices, so both sit under their parent.
                String anchorPath = readString(element, "path");
                boolean isSlice = readString(element, "sliceName") != null;
                boolean anchored = isSlice ? path.equals(anchorPath) : path.equals(parentOf(anchorPath));
                if (anchored && !placed.contains(identity)) {
                    rebuilt.add(element);
                    placed.add(identity);
                }
            }
        }
        for (JsonElement candidate : missing) {
            if (!placed.contains(identity(candidate.getAsJsonObject()))) {
                rebuilt.add(candidate);
            }
        }
        profile.getAsJsonObject("snapshot").add("element", rebuilt);
    }

    /**
     * A differential element says only what changed, while a snapshot element has to stand on its own. This
     * fills the rest in from Element.extension, which is what the omitted element derives from, and lets the
     * differential override it.
     */
    private static JsonObject asSnapshotElement(JsonObject differential, String path) {
        JsonObject element = extensionPoint(path);
        for (String member : differential.keySet()) {
            element.add(member, differential.get(member));
        }
        String sliceName = readString(differential, "sliceName");
        element.addProperty("id", sliceName == null || sliceName.isBlank() ? path : path + ":" + sliceName);
        if (!element.has("min")) {
            element.addProperty("min", 0);
        }
        if (!element.has("max")) {
            element.addProperty("max", "*");
        }
        return element;
    }

    private static String identity(JsonObject element) {
        String sliceName = readString(element, "sliceName");
        String path = readString(element, "path");
        return sliceName == null || sliceName.isBlank() ? String.valueOf(path) : path + ":" + sliceName;
    }

    private static String parentOf(String path) {
        return path == null || !path.contains(".") ? "" : path.substring(0, path.lastIndexOf('.'));
    }

    /**
     * Gives the base the extension points a differential reaches for but a resource snapshot never spells out.
     *
     * <p>Every element in FHIR carries {@code Element.extension}, primitives included, so constraining
     * {@code Patient.gender.extension} is legal. A resource's snapshot only expands complex elements though, so
     * HL7's snapshot generator finds no match for that path and drops the slice with "has an element that is
     * not marked with a snapshot match" -- leaving a profile whose differential declares an extension its
     * snapshot does not, which is what the element tables and the generator read.
     *
     * <p>Rather than hand-build the resolved slice, the missing parent is added to a copy of the base so the
     * generator can resolve it itself, exactly as it does for a complex element.
     *
     * @return the base, or a copy carrying the extension points the differential needs
     */
    private static JsonObject withExtensionPoints(JsonObject profile, JsonObject base) {
        JsonObject baseSnapshot = base != null && base.has("snapshot") && base.get("snapshot").isJsonObject()
                ? base.getAsJsonObject("snapshot") : null;
        if (baseSnapshot == null || !baseSnapshot.has("element")) {
            return base;
        }
        Set<String> wanted = new LinkedHashSet<>();
        JsonObject differential = profile.has("differential") && profile.get("differential").isJsonObject()
                ? profile.getAsJsonObject("differential") : null;
        if (differential == null || !differential.has("element")) {
            return base;
        }
        for (JsonElement entry : differential.getAsJsonArray("element")) {
            if (!entry.isJsonObject()) {
                continue;
            }
            String path = readString(entry.getAsJsonObject(), "path");
            if (path != null && path.endsWith(".extension")) {
                wanted.add(path);
            }
        }
        if (wanted.isEmpty()) {
            return base;
        }

        JsonArray existing = baseSnapshot.getAsJsonArray("element");
        Set<String> present = new LinkedHashSet<>();
        for (JsonElement entry : existing) {
            if (entry.isJsonObject()) {
                present.add(readString(entry.getAsJsonObject(), "path"));
            }
        }
        wanted.removeAll(present);
        if (wanted.isEmpty()) {
            return base;
        }

        JsonObject augmented = JsonParser.parseString(base.toString()).getAsJsonObject();
        JsonArray elements = augmented.getAsJsonObject("snapshot").getAsJsonArray("element");
        JsonArray rebuilt = new JsonArray();
        for (JsonElement entry : elements) {
            rebuilt.add(entry);
            String path = entry.isJsonObject() ? readString(entry.getAsJsonObject(), "path") : null;
            // Insert the extension point directly after the element it belongs to, so snapshot order holds.
            if (path != null && wanted.contains(path + ".extension")) {
                rebuilt.add(extensionPoint(path + ".extension"));
            }
        }
        augmented.getAsJsonObject("snapshot").add("element", rebuilt);
        return augmented;
    }

    /** Element.extension as the core spec defines it, rebased onto the element that is missing it. */
    private static JsonObject extensionPoint(String path) {
        JsonObject element = new JsonObject();
        element.addProperty("id", path);
        element.addProperty("path", path);
        element.addProperty("short", "Additional content defined by implementations");
        element.addProperty("definition",
                "May be used to represent additional information that is not part of the basic definition of "
                        + "the element.");
        element.addProperty("min", 0);
        element.addProperty("max", "*");
        JsonObject baseRef = new JsonObject();
        baseRef.addProperty("path", "Element.extension");
        baseRef.addProperty("min", 0);
        baseRef.addProperty("max", "*");
        element.add("base", baseRef);
        JsonObject type = new JsonObject();
        type.addProperty("code", "Extension");
        JsonArray types = new JsonArray();
        types.add(type);
        element.add("type", types);
        element.addProperty("isModifier", false);
        element.addProperty("isSummary", false);
        return element;
    }

    /**
     * Adds a slice to a repeating element of a profile, binding an extension when the element is an extension.
     *
     * <p>Goes through the same validate-then-regenerate path as any other edit, so a slice that is not a legal
     * constraint of the base is refused and the snapshot stays in step with the differential.
     */
    public WriteResult addSlice(String workspaceId, String fileName, String path, String sliceName, Integer min,
                                String max, boolean mustSupport, String extensionUrl, String description)
            throws IOException {
        String raw = readDefinition(workspaceId, fileName);
        JsonObject original = JsonParser.parseString(raw).getAsJsonObject();
        JsonObject edited = JsonParser.parseString(raw).getAsJsonObject();
        JsonObject base = resolveBaseDefinition(workspaceId, edited);
        ProfileEditor.addSlice(edited, path, sliceName, min, max, mustSupport, extensionUrl, description, base);

        List<ProfileRuleValidator.Finding> findings = ProfileRuleValidator.validate(edited, original, base);
        if (findings.stream().anyMatch(ProfileRuleValidator.Finding::isError)) {
            throw new ProfileRuleViolation(findings);
        }
        regenerateSnapshot(workspaceId, edited, base);
        Path file = definitionFile(workspaceId, fileName);
        writeAtomically(file, edited.toString());
        markEdited(workspaceId, fileName);
        return new WriteResult(summarise(file), findings);
    }

    /**
     * Rebuilds the profile's snapshot from its base and differential, the way a publisher would.
     *
     * <p>Best effort: a snapshot that cannot be regenerated (an unresolvable base, or an R5 profile, whose
     * generator is a different API) leaves the existing one in place. The differential is still correct, so the
     * edit is not lost -- the snapshot table just shows the pre-edit resolution until the package is rebuilt.
     */
    private void regenerateSnapshot(String workspaceId, JsonObject profile, JsonObject base) {
        try {
            Workspace workspace = describe(workspaceId);
            if (base == null || !"r4".equalsIgnoreCase(String.valueOf(workspace.fhirVersion()))) {
                return;
            }
            StructureDefinitionTableRenderer renderer = tableRenderer();
            if (renderer == null) {
                return;
            }
            JsonObject regenerated = renderer.regenerateSnapshot(profile, withExtensionPoints(profile, base));
            if (regenerated != null) {
                profile.add("snapshot", regenerated);
            }
            reconcileSnapshot(profile);
        } catch (Exception | Error e) {
            // Errors are caught too: the FHIR libraries throw bare Errors from snapshot generation, and one
            // escaping here would take down the request thread rather than degrading.
        } finally {
            // Whether generation succeeded, failed, or was skipped, the snapshot must agree with the
            // differential. It does not always: generation refuses some published profiles outright -- US
            // Core's AllergyIntolerance among them -- and the snapshot then keeps the values it shipped with,
            // so an applied change showed in the element editor and never in the rendered table.
            ProfileEditor.overlayDifferential(profile);
        }
    }

    /**
     * Renders one definition as the differential or snapshot table published IG sites show, or returns null
     * when this package's FHIR version is not one the HL7 renderer here supports, so the caller can fall back to
     * the viewer's own element table.
     */
    public String renderTable(String workspaceId, String fileName, boolean differential) throws IOException {
        Workspace workspace = describe(workspaceId);
        if (!StructureDefinitionTableRenderer.supports(workspace.fhirVersion())) {
            return null;
        }
        String json = expandedDefinition(workspaceId, fileName).toString();
        StructureDefinitionTableRenderer renderer = tableRenderer();
        if (renderer != null) {
            // Both renderers read their icons from the same directory, and anything may have removed it since
            // the last render. Putting back what is missing costs a few existence checks.
            renderer.ensureAssets();
        }
        if ("r5".equalsIgnoreCase(workspace.fhirVersion())) {
            R5TableRenderer r5 = r5TableRenderer();
            return r5 == null ? null : r5.render(json, differential);
        }
        return renderer == null ? null : renderer.render(json, differential);
    }

    /** Null when the core definitions could not be loaded; the viewer then falls back rather than erroring. */
    private StructureDefinitionTableRenderer tableRenderer() {
        StructureDefinitionTableRenderer existing = tableRenderer;
        if (existing != null || tableRendererError != null) {
            return existing;
        }
        synchronized (this) {
            if (tableRenderer == null && tableRendererError == null) {
                try {
                    tableRenderer = new StructureDefinitionTableRenderer(corePackageArchive("r4"),
                            config.getWorkspaceRoot().resolve("ig-assets"));
                } catch (Exception e) {
                    tableRendererError = e.getMessage();
                }
            }
            return tableRenderer;
        }
    }

    /** Null when the R5 core definitions could not be loaded; the viewer then falls back rather than erroring. */
    private R5TableRenderer r5TableRenderer() {
        R5TableRenderer existing = r5TableRenderer;
        if (existing != null || r5TableRendererError != null) {
            return existing;
        }
        synchronized (this) {
            if (r5TableRenderer == null && r5TableRendererError == null) {
                try {
                    // Shares the R4 renderer's unpacked icons, so the assets are written out once.
                    StructureDefinitionTableRenderer r4 = tableRenderer();
                    if (r4 == null) {
                        r5TableRendererError = "icons unavailable";
                    } else {
                        r5TableRenderer = new R5TableRenderer(corePackageArchive("r5"), r4.imageFolder());
                    }
                } catch (Exception e) {
                    r5TableRendererError = e.getMessage();
                }
            }
            return r5TableRenderer;
        }
    }

    /**
     * The FHIR R4 core package archive, downloading it through the ordinary package machinery if this is the
     * first time it is needed, so it lands in the same cache as every other package.
     */
    private Path corePackageArchive(String fhirVersion) throws BallerinaHealthException, IOException {
        IgRegistryConfig.IgPackageRef core = igCatalog.getRegistryConfig().getDefaultPackage(fhirVersion);
        String fallbackName = "r5".equalsIgnoreCase(fhirVersion) ? "hl7.fhir.r5.core" : "hl7.fhir.r4.core";
        String fallbackVersion = "r5".equalsIgnoreCase(fhirVersion) ? "5.0.0" : "4.0.1";
        String name = core == null ? fallbackName : core.name();
        String version = core == null ? fallbackVersion : core.version();
        Path archive = Path.of(igCatalog.getRegistryConfig().getCacheDir())
                .resolve(name + "-" + version + ".tgz");
        if (!Files.isRegularFile(archive)) {
            archive = config.getIgCacheDir().resolve(name + "-" + version + ".tgz");
        }
        if (!Files.isRegularFile(archive)) {
            // Downloading also writes the archive into the IG cache, which is what the renderer reads.
            Path scratch = config.getWorkspaceRoot().resolve("spec-cache")
                    .resolve(PathUtils.slug(name + "-" + version, "ig"));
            Files.createDirectories(scratch);
            igCatalog.listResourceTypes(name, version, null, scratch);
            archive = config.getIgCacheDir().resolve(name + "-" + version + ".tgz");
        }
        if (!Files.isRegularFile(archive)) {
            throw new IOException("The FHIR R4 core package is not available at " + archive);
        }
        return archive;
    }

    public Path workspaceDir(String workspaceId) {
        // Workspaces live in the same root as uploaded specifications, so the generator can consume one as an
        // ordinary local specification path with no extra plumbing.
        return config.getSpecsRoot().resolve(PathUtils.slug(workspaceId, "ig"));
    }

    /** A Ballerina-safe project name derived from the package, e.g. hl7.fhir.us.core@6.1.0 -> us_core_610. */
    public static String defaultProjectName(String packageName, String version) {
        String name = packageName == null ? "" : packageName;
        for (String prefix : List.of("hl7.fhir.", "hl7.")) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
                break;
            }
        }
        String base = name.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
        String versionPart = version == null ? "" : version.replaceAll("[^0-9]", "");
        String candidate = (base.isEmpty() ? "fhir" : base) + (versionPart.isEmpty() ? "" : "_" + versionPart);
        if (candidate.isEmpty() || !Character.isLetter(candidate.charAt(0))) {
            candidate = "ig_" + candidate;
        }
        return candidate;
    }

    private String defaultProjectName(Metadata metadata) {
        return defaultProjectName(metadata.packageName(), metadata.version());
    }

    private Path pristineDir(String packageName, String version, String registryUrlOverride)
            throws BallerinaHealthException, IOException {
        Path cacheDir = config.getWorkspaceRoot().resolve("spec-cache")
                .resolve(PathUtils.slug(packageName + "-" + version, "ig"));
        Files.createDirectories(cacheDir);
        // Downloading through the catalog service keeps one extraction shared with the API listing and with
        // any job that runs against the same package.
        igCatalog.listResourceTypes(packageName, version, registryUrlOverride, cacheDir);
        Path specRoot = cacheDir.resolve("spec");
        if (Files.isDirectory(specRoot)) {
            try (Stream<Path> children = Files.list(specRoot)) {
                Path first = children.filter(Files::isDirectory).findFirst().orElse(null);
                if (first != null) {
                    return first;
                }
            }
        }
        return cacheDir;
    }

    /** Copies the definition files only; the extraction also holds package metadata the generator ignores. */
    private void copyDefinitions(Path from, Path to) throws IOException {
        Files.createDirectories(to);
        try (Stream<Path> files = Files.walk(from)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Path target = to.resolve(file.getFileName().toString());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private Definition summarise(Path file) {
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject json = parsed.getAsJsonObject();
            if (!"StructureDefinition".equals(readString(json, "resourceType"))) {
                return null;
            }
            return new Definition(
                    file.getFileName().toString(),
                    readString(json, "id"),
                    readString(json, "name"),
                    readString(json, "title"),
                    readString(json, "url"),
                    readString(json, "type"),
                    readString(json, "kind"),
                    readString(json, "derivation"),
                    readString(json, "status"),
                    readString(json, "description"),
                    readString(json, "baseDefinition"),
                    readString(json, "fhirVersion"),
                    contexts(json));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The FHIR version the package targets, taken from the definitions themselves (each StructureDefinition
     * records the version it was authored against) and reduced to the short form the tool uses elsewhere.
     */
    private static String fhirVersionOf(List<Definition> definitions) {
        for (Definition definition : definitions) {
            String raw = definition.fhirVersion();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            if (raw.startsWith("4.0")) {
                return "r4";
            }
            if (raw.startsWith("4.3")) {
                return "r4b";
            }
            if (raw.startsWith("5.")) {
                return "r5";
            }
            if (raw.startsWith("3.")) {
                return "stu3";
            }
        }
        return null;
    }

    private Path definitionFile(String workspaceId, String fileName) {
        // Names come from the client, so refuse anything that could escape the workspace directory.
        if (fileName == null || fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")
                || fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid definition file name: " + fileName);
        }
        Path file = workspaceDir(workspaceId).resolve(fileName).normalize();
        if (!file.startsWith(workspaceDir(workspaceId).normalize())) {
            throw new IllegalArgumentException("Invalid definition file name: " + fileName);
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("No such definition: " + fileName);
        }
        return file;
    }

    // ------------------------------------------------------------------ metadata

    private Path metadataFile(String workspaceId) {
        return config.getSpecsRoot().resolve(PathUtils.slug(workspaceId, "ig") + METADATA_FILE);
    }

    private void writeMetadata(String workspaceId, String packageName, String version, String registryUrl,
                               String owner) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("packageName", packageName);
        json.addProperty("version", version);
        json.addProperty("registryUrl", registryUrl);
        json.addProperty("owner", owner);
        json.addProperty("touchedAt", Instant.now().toString());
        json.add("edited", new JsonArray());
        writeAtomically(metadataFile(workspaceId), json.toString());
    }

    /**
     * Records that the workspace is still in use, so the sweep does not reclaim it out from under someone.
     *
     * <p>Only written when the recorded time has actually gone stale. It used to be rewritten on every request,
     * which meant the file every request also *reads* was being replaced underneath them -- and a burst of
     * requests, exactly what applying an edit produces, would catch it half-written. The sweep works in days;
     * a timestamp good to the hour is ample.
     */
    public void touch(String workspaceId) {
        try {
            Path file = metadataFile(workspaceId);
            if (!Files.isRegularFile(file)) {
                return;
            }
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject json = parsed.getAsJsonObject();
            String recorded = readString(json, "touchedAt");
            if (recorded != null) {
                try {
                    if (Instant.parse(recorded).isAfter(Instant.now().minus(TOUCH_INTERVAL))) {
                        return;
                    }
                } catch (Exception ignored) {
                    // An unreadable timestamp is replaced below.
                }
            }
            json.addProperty("touchedAt", Instant.now().toString());
            writeAtomically(file, json.toString());
        } catch (Exception e) {
            // Losing a timestamp costs an early sweep at worst, which is not worth failing a request over.
        }
    }

    private Metadata readMetadata(String workspaceId) throws IOException {
        Path file = metadataFile(workspaceId);
        if (!Files.isRegularFile(file)) {
            // An uploaded archive has no registry identity. The upload records the name the user gave it beside
            // the directory, so prefer that over the generated id, which makes for a far better project name.
            Path nameFile = config.getSpecsRoot().resolve(workspaceId + ".name");
            String uploadedName = Files.isRegularFile(nameFile)
                    ? Files.readString(nameFile, StandardCharsets.UTF_8).trim() : "";
            return new Metadata(uploadedName.isEmpty() ? workspaceId : uploadedName, "", null, List.of(),
                    readUploadOwner(workspaceId), null);
        }
        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> edited = new ArrayList<>();
        if (json.has("edited") && json.get("edited").isJsonArray()) {
            json.getAsJsonArray("edited").forEach(e -> edited.add(e.getAsString()));
        }
        return new Metadata(readString(json, "packageName"), readString(json, "version"),
                readString(json, "registryUrl"), edited, readString(json, "owner"),
                readString(json, "touchedAt"));
    }

    private void markEdited(String workspaceId, String fileName) throws IOException {
        Path file = metadataFile(workspaceId);
        if (!Files.isRegularFile(file)) {
            return;
        }
        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray edited = json.has("edited") && json.get("edited").isJsonArray()
                ? json.getAsJsonArray("edited") : new JsonArray();
        boolean present = false;
        for (JsonElement element : edited) {
            if (fileName.equals(element.getAsString())) {
                present = true;
                break;
            }
        }
        if (!present) {
            edited.add(fileName);
        }
        json.add("edited", edited);
        writeAtomically(file, json.toString());
    }

    /**
     * The owner of an uploaded archive, kept beside the directory because an upload has no package metadata
     * file of its own. Absent for archives uploaded before ownership existed, which stay shared.
     */
    private String readUploadOwner(String workspaceId) {
        try {
            Path file = config.getSpecsRoot().resolve(workspaceId + ".owner");
            return Files.isRegularFile(file)
                    ? Files.readString(file, StandardCharsets.UTF_8).trim() : null;
        } catch (IOException e) {
            return null;
        }
    }

    /** Records who uploaded an archive, so it is theirs the way an opened package is. */
    public void recordUploadOwner(String workspaceId, String owner) throws IOException {
        if (owner == null || owner.isBlank()) {
            return;
        }
        Files.writeString(config.getSpecsRoot().resolve(workspaceId + ".owner"), owner, StandardCharsets.UTF_8);
    }

    public List<String> editedFiles(String workspaceId) throws IOException {
        return readMetadata(workspaceId).edited();
    }

    // ------------------------------------------------------------------ replay

    /**
     * Puts a stored differential back onto a definition, re-deriving everything that follows from it.
     *
     * <p>The browser holds the edits, and it holds them as differentials: the snapshot is derived, so keeping a
     * copy of it would be storing the same information twice in a form sixty times larger. That makes reopening
     * a package a matter of handing each differential back and letting the service rebuild the snapshot exactly
     * as it does after an ordinary edit -- including validating it against the base, so an edit that was legal
     * when made cannot come back as something that is not.
     */
    public WriteResult applyDifferential(String workspaceId, String fileName, JsonObject differential)
            throws IOException {
        if (differential == null) {
            throw new IllegalArgumentException("A 'differential' is required.");
        }
        Path file = definitionFile(workspaceId, fileName);
        JsonObject original = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonObject edited = JsonParser.parseString(original.toString()).getAsJsonObject();
        edited.add("differential", differential);

        JsonObject base = resolveBaseDefinition(workspaceId, edited);
        List<ProfileRuleValidator.Finding> findings = ProfileRuleValidator.validate(edited, original, base);
        if (findings.stream().anyMatch(ProfileRuleValidator.Finding::isError)) {
            throw new ProfileRuleViolation(findings);
        }
        regenerateSnapshot(workspaceId, edited, base);
        writeAtomically(file, edited.toString());
        markEdited(workspaceId, fileName);
        return new WriteResult(summarise(file), findings);
    }

    /**
     * Writes a definition so a reader never sees it half-written.
     *
     * <p>{@code Files.writeString} truncates and then fills, so anything reading the same file in between gets
     * an empty or partial document -- which surfaces as "Not a JSON Object: null" from the parser, at a moment
     * that looks unrelated to the edit that caused it. Writing beside the file and moving it into place makes
     * the change atomic: a reader sees either the old definition or the new one.
     */
    private static void writeAtomically(Path file, String content) throws IOException {
        Path temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Rare, and still better than truncating the original in place.
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static boolean isEmpty(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.findAny().isEmpty();
        }
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

    /** Summary of one StructureDefinition in a workspace. */
    public record Definition(String file, String id, String name, String title, String url, String resourceType,
                             String kind, String derivation, String status, String description,
                             String baseDefinition, String fhirVersion, List<String> contexts) {
    }

    /** An opened package: its identity, size, and the project name a facade would default to. */
    public record Workspace(String workspaceId, String packageName, String version, String registryUrl,
                            String fhirVersion, int definitionCount, String defaultProjectName, int editedCount,
                            String title, String description, String publisher, String canonical) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("workspaceId", workspaceId);
            map.put("packageName", packageName);
            map.put("version", version);
            map.put("registryUrl", registryUrl);
            map.put("fhirVersion", fhirVersion);
            map.put("definitionCount", definitionCount);
            map.put("defaultProjectName", defaultProjectName);
            map.put("editedCount", editedCount);
            map.put("title", title);
            map.put("description", description);
            map.put("publisher", publisher);
            map.put("canonical", canonical);
            // Uploaded archives have no published version, so the viewer hides "discard edits" for them.
            map.put("resettable", version != null && !version.isBlank());
            return map;
        }
    }

    private record Metadata(String packageName, String version, String registryUrl, List<String> edited,
                            String owner, String touchedAt) {
        int editedCount() {
            return edited == null ? 0 : edited.size();
        }
    }
}
