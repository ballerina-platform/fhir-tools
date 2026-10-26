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

package io.ballerina.health.service.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.ballerina.health.service.ServiceConfig;
import io.ballerina.health.service.job.GenerationRequest;
import io.ballerina.health.service.job.Job;
import io.ballerina.health.service.chat.ChatService;
import io.ballerina.health.service.job.JobManager;
import io.ballerina.health.service.job.JobProgress;
import com.google.gson.JsonObject;
import io.ballerina.health.service.registry.ExtensionAuthor;
import io.ballerina.health.service.registry.IgCatalogService;
import io.ballerina.health.service.registry.PackageWorkspaceService;
import io.ballerina.health.service.registry.ProfileRuleValidator;
import io.ballerina.health.service.util.FileTrees;
import io.ballerina.health.service.util.PathUtils;
import io.ballerina.health.service.util.Zips;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST surface of the codegen service, rooted at {@code /api}.
 */
public class ApiHandler implements HttpHandler {

    /** Names the calling browser. Set by the UI on every request; see clientOf. */
    private static final String CLIENT_HEADER = "X-Client-Id";

    private final ChatService chat;

    private final ServiceConfig config;
    private final JobManager jobManager;
    private final IgCatalogService igCatalog;
    private final PackageWorkspaceService workspaces;
    private final Http http;
    private final String version;

    public ApiHandler(ServiceConfig config, JobManager jobManager, IgCatalogService igCatalog,
                      PackageWorkspaceService workspaces, Http http, String version) {
        this.chat = new ChatService(config, workspaces);
        this.config = config;
        this.jobManager = jobManager;
        this.igCatalog = igCatalog;
        this.workspaces = workspaces;
        this.http = http;
        this.version = version;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (http.handlePreflight(exchange)) {
                return;
            }
            route(exchange);
        } catch (IllegalArgumentException e) {
            http.writeError(exchange, 400, e.getMessage());
        } catch (JobManager.ValidationException e) {
            http.writeError(exchange, 422, e.getMessage());
        } catch (Exception e) {
            // An unexpected failure used to reach the browser as nothing but its message, and a message like
            // Gson's "Not a JSON Object: null" says neither what failed nor where. The stack goes to the
            // service log, and the reply names the request and the exception type so the two can be matched up.
            String where = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
            System.err.println("[ERROR] Unhandled failure handling " + where);
            e.printStackTrace();
            String detail = e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName()
                    : e.getClass().getSimpleName() + ": " + e.getMessage();
            http.writeError(exchange, 500, detail + " (while handling " + where
                    + " — the service log has the stack trace)");
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();
        String relative = path.startsWith("/api") ? path.substring(4) : path;
        if (relative.isEmpty()) {
            relative = "/";
        }
        List<String> segments = new ArrayList<>();
        for (String segment : relative.split("/")) {
            if (!segment.isBlank()) {
                segments.add(segment);
            }
        }

        if (segments.isEmpty()) {
            http.writeError(exchange, 404, "Unknown API endpoint: " + path);
            return;
        }

        switch (segments.get(0)) {
            case "meta" -> requireGet(exchange, method, this::getMeta);
            case "model" -> requireGet(exchange, method, e -> http.writeJson(e, 200, chat.status()));
            case "stages" -> requireGet(exchange, method, e -> {
                List<Map<String, Object>> stages = new ArrayList<>();
                for (JobProgress.Stage stage : JobProgress.stages()) {
                    stages.add(Map.of("key", stage.key(), "title", stage.title(),
                            "percent", stage.percent()));
                }
                http.writeJson(e, 200, Map.of("stages", stages));
            });
            case "igs" -> handleIgs(exchange, method, segments);
            case "packages" -> handlePackages(exchange, method, segments, clientOf(exchange));
            case "registries" -> requireGet(exchange, method, this::listRegistries);
            case "specs" -> handleSpecs(exchange, method, segments, clientOf(exchange));
            case "jobs" -> handleJobs(exchange, method, segments);
            default -> http.writeError(exchange, 404, "Unknown API endpoint: " + path);
        }
    }

    // ---------------------------------------------------------------- meta

    private void getMeta(HttpExchange exchange) throws IOException {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("service", "fhir-codegen-service");
        meta.put("version", version);
        // The UI is a FHIR facade generator: it offers template generation only. Package generation stays
        // reachable through the CLI (bal health fhir -m package), which is where it belongs.
        meta.put("modes", List.of("template"));
        meta.put("sourceTypes", List.of("registry", "upload", "default"));
        meta.put("registryUrl", igCatalog.getRegistryConfig().getRegistryUrl());
        meta.put("defaultPackages", igCatalog.getDefaultPackages());
        meta.put("suggestedIgs", igCatalog.getSuggestedIgs());
        meta.put("defaults", Map.of(
                "orgName", "ballerinax",
                "packageVersion", "1.0.0",
                "aggregate", true,
                "minimal", false));
        meta.put("workerThreads", config.getWorkerThreads());
        meta.put("jobTimeoutSeconds", config.getJobTimeoutSeconds());
        http.writeJson(exchange, 200, meta);
    }

    // ---------------------------------------------------------------- igs

    private void handleIgs(HttpExchange exchange, String method, List<String> segments) throws Exception {
        if (segments.size() == 1) {
            requireGet(exchange, method, e -> http.writeJson(e, 200,
                    Map.of("igs", igCatalog.getSuggestedIgs(),
                            "registryUrl", igCatalog.getRegistryConfig().getRegistryUrl())));
            return;
        }
        if (segments.size() == 2 && "search".equals(segments.get(1))) {
            requireGet(exchange, method, this::searchIgs);
            return;
        }
        if (segments.size() == 3 && "resources".equals(segments.get(2))) {
            requireGet(exchange, method, e -> listIgResourceTypes(e, segments.get(1)));
            return;
        }
        if (segments.size() == 3 && "versions".equals(segments.get(2))) {
            if (!"GET".equals(method)) {
                http.writeError(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> params = Http.queryParams(exchange);
            String packageName = segments.get(1);
            try {
                IgCatalogService.PackageVersions versions = igCatalog.getVersions(
                        packageName, params.get("registryUrl"), Http.boolParam(params, "refresh", false));
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("name", versions.name());
                body.put("latest", versions.latest());
                body.put("versions", versions.versions());
                body.put("description", versions.description());
                body.put("fhirVersion", versions.fhirVersion());
                // packageMappings are keyed by <igName>@<igVersion>, so report the mapping for the version the
                // UI preselects (latest) rather than for the bare package name.
                body.put("mappedPackage", igCatalog.resolveDependentPackage(packageName, versions.latest()));
                http.writeJson(exchange, 200, body);
            } catch (IgCatalogService.PackageNotFoundException e) {
                http.writeError(exchange, 404, e.getMessage());
            } catch (Exception e) {
                http.writeError(exchange, 502, "Unable to read registry metadata for '" + packageName + "': "
                        + describe(e));
            }
            return;
        }
        http.writeError(exchange, 404, "Unknown IG endpoint");
    }

    /**
     * Full-text search over the package registry catalog, so the picker is not limited to the seeded
     * suggestions. Short queries are rejected because an empty catalog query returns every published package.
     */
    private void searchIgs(HttpExchange exchange) throws IOException {
        Map<String, String> params = Http.queryParams(exchange);
        String query = params.getOrDefault("q", "").trim();
        if (query.length() < 2) {
            http.writeJson(exchange, 200, Map.of("query", query, "results", List.of(),
                    "message", "Type at least two characters to search the registry."));
            return;
        }
        try {
            List<IgCatalogService.CatalogEntry> results = igCatalog.search(query, params.get("registryUrl"));
            int limit = 50;
            http.writeJson(exchange, 200, Map.of(
                    "query", query,
                    "total", results.size(),
                    "results", results.size() > limit ? results.subList(0, limit) : results));
        } catch (Exception e) {
            http.writeJson(exchange, 200, Map.of(
                    "query", query,
                    "total", 0,
                    "results", List.of(),
                    "message", "This registry did not answer a catalog search (" + describe(e)
                            + "). Enter the exact package id to open it directly."));
        }
    }

    /**
     * The FHIR resource types a package profiles, i.e. the APIs a generated facade would expose. Downloading the
     * package to answer this also warms the same spec-cache directory a job for it would use.
     */
    private void listIgResourceTypes(HttpExchange exchange, String packageName) throws IOException {
        Map<String, String> params = Http.queryParams(exchange);
        String version = params.get("version");
        try {
            if (version == null || version.isBlank() || "latest".equalsIgnoreCase(version)) {
                version = igCatalog.pinVersion(packageName, version, params.get("registryUrl"));
            }
            Path cacheDir = config.getWorkspaceRoot().resolve("spec-cache")
                    .resolve(PathUtils.slug(packageName + "-" + version, "ig"));
            Files.createDirectories(cacheDir);
            List<String> resourceTypes = igCatalog.listResourceTypes(
                    packageName, version, params.get("registryUrl"), cacheDir);
            http.writeJson(exchange, 200, Map.of(
                    "name", packageName,
                    "version", version,
                    "resourceTypes", resourceTypes));
        } catch (IgCatalogService.PackageNotFoundException e) {
            http.writeError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            http.writeError(exchange, 502,
                    "Unable to list the APIs of '" + packageName + "': " + describe(e));
        }
    }

    // ---------------------------------------------------------------- registries

    /**
     * The registry the service is configured to use. The picker offers this or a URL the user types, so only
     * the default is named here; any registry implementing the FHIR package registry API works.
     */
    private void listRegistries(HttpExchange exchange) throws IOException {
        String configured = igCatalog.getRegistryConfig().getRegistryUrl();
        http.writeJson(exchange, 200, Map.of(
                "registries", List.of(Map.of("url", configured, "name", "Default package registry",
                        "isDefault", true)),
                "defaultUrl", configured));
    }

    // ---------------------------------------------------------------- packages

    /**
     * Routes for exploring an opened package: its definitions, edits to them, and generating a facade from the
     * result. {@code POST /api/packages/open} takes a registry package; every other route addresses a workspace.
     */
    private void handlePackages(HttpExchange exchange, String method, List<String> segments, String owner)
            throws Exception {
        if (segments.size() == 2 && "open".equals(segments.get(1)) && "POST".equals(method)) {
            openPackage(exchange);
            return;
        }
        if (segments.size() < 2) {
            http.writeError(exchange, 404, "Unknown package endpoint");
            return;
        }
        String workspaceId = segments.get(1);
        // Every route below addresses someone's working copy, so ownership is checked once, here, rather than
        // being left to each branch to remember.
        workspaces.requireOwner(workspaceId, owner);
        workspaces.touch(workspaceId);
        if (segments.size() == 2 && "GET".equals(method)) {
            http.writeJson(exchange, 200, workspaces.describe(workspaceId).toMap());
            return;
        }
        if (segments.size() == 3 && "definitions".equals(segments.get(2)) && "GET".equals(method)) {
            List<Map<String, Object>> definitions = new ArrayList<>();
            List<String> edited = workspaces.editedFiles(workspaceId);
            for (PackageWorkspaceService.Definition definition : workspaces.listDefinitions(workspaceId)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("file", definition.file());
                entry.put("id", definition.id());
                entry.put("name", definition.name());
                entry.put("title", definition.title());
                entry.put("url", definition.url());
                entry.put("resourceType", definition.resourceType());
                entry.put("kind", definition.kind());
                entry.put("derivation", definition.derivation());
                entry.put("status", definition.status());
                entry.put("description", definition.description());
                entry.put("baseDefinition", definition.baseDefinition());
                entry.put("contexts", definition.contexts());
                entry.put("edited", edited.contains(definition.file()));
                definitions.add(entry);
            }
            http.writeJson(exchange, 200, Map.of("workspaceId", workspaceId, "definitions", definitions));
            return;
        }
        if (segments.size() == 4 && "definitions".equals(segments.get(2))) {
            String file = segments.get(3);
            switch (method) {
                case "GET" -> http.writeJson(exchange, 200, Map.of(
                        "file", file,
                        "content", workspaces.readDefinition(workspaceId, file)));
                case "PUT" -> {
                    String content = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    try {
                        PackageWorkspaceService.WriteResult result =
                                workspaces.writeDefinition(workspaceId, file, content);
                        http.writeJson(exchange, 200, Map.of(
                                "file", file,
                                "saved", true,
                                "findings", result.findings().stream()
                                        .map(ProfileRuleValidator.Finding::toMap).toList()));
                    } catch (PackageWorkspaceService.ProfileRuleViolation e) {
                        // 422: the JSON is well formed, it just is not a legal profile of its base.
                        http.writeJson(exchange, 422, Map.of(
                                "file", file,
                                "saved", false,
                                "error", e.getMessage(),
                                "findings", e.getFindings().stream()
                                        .map(ProfileRuleValidator.Finding::toMap).toList()));
                    }
                }
                default -> http.writeError(exchange, 405, "Method not allowed");
            }
            return;
        }
        if (segments.size() == 5 && "definitions".equals(segments.get(2)) && "elements".equals(segments.get(4))) {
            String file = segments.get(3);
            switch (method) {
                case "GET" -> http.writeJson(exchange, 200, Map.of(
                        "file", file,
                        "elements", workspaces.describeElements(workspaceId, file),
                        "bindingStrengths", ProfileRuleValidator.bindingStrengths()));
                case "POST" -> {
                    JsonObject body = http.readJson(exchange, JsonObject.class);
                    if (body == null || !body.has("path")) {
                        throw new IllegalArgumentException("An element 'path' is required.");
                    }
                    String path = body.get("path").getAsString();
                    String sliceName = body.has("sliceName") && !body.get("sliceName").isJsonNull()
                            ? body.get("sliceName").getAsString() : null;
                    JsonObject changes = body.has("changes") && body.get("changes").isJsonObject()
                            ? body.getAsJsonObject("changes") : new JsonObject();
                    try {
                        PackageWorkspaceService.WriteResult result =
                                workspaces.applyElementChanges(workspaceId, file, path, sliceName, changes);
                        http.writeJson(exchange, 200, Map.of(
                                "file", file,
                                "saved", true,
                                "findings", result.findings().stream()
                                        .map(ProfileRuleValidator.Finding::toMap).toList()));
                    } catch (PackageWorkspaceService.ProfileRuleViolation e) {
                        http.writeJson(exchange, 422, Map.of(
                                "file", file,
                                "saved", false,
                                "error", e.getMessage(),
                                "findings", e.getFindings().stream()
                                        .map(ProfileRuleValidator.Finding::toMap).toList()));
                    }
                }
                default -> http.writeError(exchange, 405, "Method not allowed");
            }
            return;
        }
        if (segments.size() == 5 && "definitions".equals(segments.get(2)) && "slices".equals(segments.get(4))
                && "POST".equals(method)) {
            String file = segments.get(3);
            JsonObject body = http.readJson(exchange, JsonObject.class);
            if (body == null || !body.has("path") || !body.has("sliceName")) {
                throw new IllegalArgumentException("A 'path' and 'sliceName' are required.");
            }
            try {
                PackageWorkspaceService.WriteResult result = workspaces.addSlice(
                        workspaceId,
                        file,
                        body.get("path").getAsString(),
                        body.get("sliceName").getAsString(),
                        body.has("min") && !body.get("min").isJsonNull() ? body.get("min").getAsInt() : null,
                        optional(body, "max"),
                        body.has("mustSupport") && body.get("mustSupport").getAsBoolean(),
                        optional(body, "extensionUrl"),
                        optional(body, "description"));
                http.writeJson(exchange, 201, Map.of(
                        "file", file,
                        "created", true,
                        "findings", result.findings().stream()
                                .map(ProfileRuleValidator.Finding::toMap).toList()));
            } catch (PackageWorkspaceService.ProfileRuleViolation e) {
                http.writeJson(exchange, 422, Map.of(
                        "file", file,
                        "created", false,
                        "error", e.getMessage(),
                        "findings", e.getFindings().stream()
                                .map(ProfileRuleValidator.Finding::toMap).toList()));
            }
            return;
        }
        if (segments.size() == 5 && "definitions".equals(segments.get(2))
                && "differential".equals(segments.get(4)) && "PUT".equals(method)) {
            // Replaying an edit the browser is holding. The differential is the whole of it; everything else
            // about the definition is either pristine or derived from it.
            JsonObject body = http.readJson(exchange, JsonObject.class);
            if (body == null || !body.has("differential") || !body.get("differential").isJsonObject()) {
                throw new IllegalArgumentException("A 'differential' object is required.");
            }
            try {
                PackageWorkspaceService.WriteResult result = workspaces.applyDifferential(
                        workspaceId, segments.get(3), body.getAsJsonObject("differential"));
                http.writeJson(exchange, 200, Map.of("definition", result.definition(),
                        "findings", result.findings()));
            } catch (PackageWorkspaceService.ProfileRuleViolation e) {
                http.writeJson(exchange, 422, Map.of("error", e.getMessage(), "findings", e.getFindings()));
            }
            return;
        }
        if (segments.size() == 5 && "definitions".equals(segments.get(2)) && "table".equals(segments.get(4))
                && "GET".equals(method)) {
            String file = segments.get(3);
            boolean differential = !"snapshot".equalsIgnoreCase(Http.queryParams(exchange).get("view"));
            String html = workspaces.renderTable(workspaceId, file, differential);
            // A null table means this package's FHIR version has no HL7 renderer here; the client falls back to
            // its own element table rather than showing an error.
            http.writeJson(exchange, 200, html == null
                    ? Map.of("file", file, "supported", false)
                    : Map.of("file", file, "supported", true, "html", html));
            return;
        }
        if (segments.size() == 3 && "extensions".equals(segments.get(2))) {
            switch (method) {
                case "GET" -> http.writeJson(exchange, 200, Map.of(
                        "valueTypes", ExtensionAuthor.VALUE_TYPES));
                case "POST" -> {
                    JsonObject body = http.readJson(exchange, JsonObject.class);
                    if (body == null || !body.has("id")) {
                        throw new IllegalArgumentException("An extension 'id' is required.");
                    }
                    try {
                        PackageWorkspaceService.Definition created = workspaces.createExtension(
                                workspaceId,
                                body.get("id").getAsString(),
                                optional(body, "name"),
                                optional(body, "title"),
                                optional(body, "description"),
                                stringList(body, "contexts"),
                                stringList(body, "valueTypes"),
                                body.has("required") && body.get("required").getAsBoolean(),
                                body.has("repeats") && body.get("repeats").getAsBoolean());
                        // The url and title come back so a caller that just created one can select it
                        // straight away, without waiting for the definition list to reload.
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("created", true);
                        response.put("file", created.file());
                        response.put("id", created.id());
                        response.put("name", created.name());
                        response.put("title", created.title());
                        response.put("url", created.url());
                        response.put("contexts", created.contexts());
                        http.writeJson(exchange, 201, response);
                    } catch (PackageWorkspaceService.ProfileRuleViolation e) {
                        http.writeJson(exchange, 422, Map.of(
                                "created", false,
                                "error", e.getMessage(),
                                "findings", e.getFindings().stream()
                                        .map(ProfileRuleValidator.Finding::toMap).toList()));
                    }
                }
                default -> http.writeError(exchange, 405, "Method not allowed");
            }
            return;
        }
        if (segments.size() == 3 && "chat".equals(segments.get(2)) && "POST".equals(method)) {
            JsonObject body = http.readJson(exchange, JsonObject.class);
            if (body == null || !body.has("messages") || !body.get("messages").isJsonArray()) {
                throw new IllegalArgumentException("'messages' is required.");
            }
            try {
                http.writeJson(exchange, 200, chat.ask(workspaceId, optional(body, "file"),
                        body.getAsJsonArray("messages"),
                        body.has("focus") && body.get("focus").isJsonArray()
                                ? body.getAsJsonArray("focus") : null,
                        body.has("applied") && body.get("applied").isJsonArray()
                                ? body.getAsJsonArray("applied") : null));
            } catch (IllegalStateException e) {
                // Asked for something the service does not offer, rather than something that went wrong.
                http.writeError(exchange, 501, e.getMessage());
            } catch (IOException e) {
                // Reaching the model is the usual failure, and it is the reader's own to fix.
                http.writeError(exchange, 503, e.getMessage());
            }
            return;
        }
        if (segments.size() == 3 && "profiles".equals(segments.get(2))) {
            switch (method) {
                // What a new profile could be based on: this package's resources, then the FHIR core's.
                case "GET" -> http.writeJson(exchange, 200,
                        Map.of("bases", workspaces.baseProfiles(workspaceId)));
                case "POST" -> {
                    JsonObject body = http.readJson(exchange, JsonObject.class);
                    if (body == null || !body.has("id") || !body.has("baseUrl")) {
                        throw new IllegalArgumentException("A profile needs an 'id' and a 'baseUrl'.");
                    }
                    try {
                        PackageWorkspaceService.Definition created = workspaces.createProfile(workspaceId,
                                body.get("id").getAsString(), optional(body, "title"),
                                optional(body, "description"), body.get("baseUrl").getAsString());
                        http.writeJson(exchange, 201, Map.of("created", true, "file", created.file(),
                                "id", String.valueOf(created.id()), "name", String.valueOf(created.name()),
                                "url", String.valueOf(created.url()),
                                "resourceType", String.valueOf(created.resourceType())));
                    } catch (PackageWorkspaceService.ProfileRuleViolation e) {
                        http.writeJson(exchange, 422, Map.of("error", e.getMessage(),
                                "findings", e.getFindings()));
                    }
                }
                default -> http.writeError(exchange, 405, "Method not allowed");
            }
            return;
        }
        if (segments.size() == 3 && "archive".equals(segments.get(2)) && "GET".equals(method)) {
            downloadPackage(exchange, workspaceId);
            return;
        }
        if (segments.size() == 3 && "reset".equals(segments.get(2)) && "POST".equals(method)) {
            http.writeJson(exchange, 200, workspaces.reset(workspaceId).toMap());
            return;
        }
        if (segments.size() == 3 && "generate".equals(segments.get(2)) && "POST".equals(method)) {
            generateFacade(exchange, workspaceId);
            return;
        }
        http.writeError(exchange, 404, "Unknown package endpoint");
    }

    /**
     * Which browser is calling, so its working copies stay its own.
     *
     * <p>Absent for anything that is not this UI -- a curl, an older client -- which then gets the shared,
     * unowned behaviour rather than an error.
     */
    private static String clientOf(HttpExchange exchange) {
        String value = exchange.getRequestHeaders().getFirst(CLIENT_HEADER);
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        // The id reaches a file name by way of a hash, but it is still untrusted input: keep it bounded.
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    /**
     * Streams the package as it now stands, edits included.
     *
     * <p>Laid out the way a FHIR package is -- everything under {@code package/} -- so it is not merely a
     * folder of files but something other FHIR tooling will accept, and something this tool will take back
     * through its own upload.
     */
    private void downloadPackage(HttpExchange exchange, String workspaceId) throws Exception {
        PackageWorkspaceService.Workspace workspace = workspaces.describe(workspaceId);
        Path directory = workspaces.workspaceDir(workspaceId);
        if (!Files.isDirectory(directory)) {
            http.writeError(exchange, 404, "This package has no files.");
            return;
        }
        String fallback = (workspace.packageName() == null ? workspaceId : workspace.packageName())
                + (workspace.version() == null || workspace.version().isBlank() ? "" : "-" + workspace.version())
                + (workspace.editedCount() > 0 ? "-edited" : "");
        // The folder inside the archive is named too, not just the archive. Unzipped twice, a folder called
        // "package" becomes "package 2" and says nothing about which package it is.
        String asked = Http.queryParams(exchange).get("name");
        String folder = safeFolderName(asked == null || asked.isBlank() ? fallback : asked);
        Map<String, String> headers = Map.of(
                "Content-Disposition", "attachment; filename=\"" + folder + ".zip\"");
        try (OutputStream out = http.beginStream(exchange, 200, "application/zip", headers)) {
            Zips.zipDirectory(directory, folder, out);
        }
    }

    private static String optional(JsonObject body, String member) {
        return body.has(member) && !body.get(member).isJsonNull() ? body.get(member).getAsString() : null;
    }

    private static List<String> stringList(JsonObject body, String member) {
        List<String> values = new ArrayList<>();
        if (body.has(member) && body.get(member).isJsonArray()) {
            body.getAsJsonArray(member).forEach(entry -> values.add(entry.getAsString()));
        }
        return values;
    }

    private void openPackage(HttpExchange exchange) throws Exception {
        JsonObject body = http.readJson(exchange, JsonObject.class);
        if (body == null || !body.has("name")) {
            throw new IllegalArgumentException("A package 'name' is required.");
        }
        String name = body.get("name").getAsString();
        String requestedVersion = body.has("version") && !body.get("version").isJsonNull()
                ? body.get("version").getAsString() : null;
        String registryUrl = body.has("registryUrl") && !body.get("registryUrl").isJsonNull()
                ? body.get("registryUrl").getAsString() : null;
        try {
            http.writeJson(exchange, 200,
                    workspaces.open(name, requestedVersion, registryUrl, clientOf(exchange)).toMap());
        } catch (IgCatalogService.PackageNotFoundException e) {
            http.writeError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            http.writeError(exchange, 502, "Unable to open '" + name + "': " + describe(e));
        }
    }

    /**
     * Generates a facade from the workspace as it currently stands, edits included. The workspace is an ordinary
     * local specification directory, so this is a plain template run with no options to choose: the project name
     * is the only thing the caller can set, and it defaults to one derived from the package.
     */
    private void generateFacade(HttpExchange exchange, String workspaceId) throws Exception {
        JsonObject body = http.readJson(exchange, JsonObject.class);
        PackageWorkspaceService.Workspace workspace = workspaces.describe(workspaceId);
        String projectName = body != null && body.has("projectName") && !body.get("projectName").isJsonNull()
                ? body.get("projectName").getAsString().trim() : "";
        if (projectName.isEmpty()) {
            projectName = workspace.defaultProjectName();
        }

        GenerationRequest request = new GenerationRequest();
        request.setMode("template");
        GenerationRequest.SpecSource source = new GenerationRequest.SpecSource();
        source.setType("upload");
        source.setSpecId(workspaceId);
        request.setSource(source);
        request.setPackageName(projectName);
        request.setAggregate(true);
        request.setLabel(workspace.packageName() + "@" + workspace.version());
        Job job = jobManager.submit(request);
        http.writeJson(exchange, 202, jobDetail(job));
    }

    // ---------------------------------------------------------------- specs

    private void handleSpecs(HttpExchange exchange, String method, List<String> segments, String owner)
            throws Exception {
        if (segments.size() == 1 && "GET".equals(method)) {
            http.writeJson(exchange, 200, Map.of("specs", listSpecs(owner)));
            return;
        }
        if (segments.size() == 1 && "POST".equals(method)) {
            uploadSpec(exchange, owner);
            return;
        }
        if (segments.size() == 2 && "DELETE".equals(method)) {
            Path specDir = config.getSpecsRoot().resolve(segments.get(1));
            // Someone else's upload is not theirs to delete, and is reported as simply not being there.
            if (!workspaces.isVisibleTo(segments.get(1), owner)) {
                http.writeError(exchange, 404, "Unknown specification");
                return;
            }
            if (!Files.isDirectory(specDir)) {
                http.writeError(exchange, 404, "Unknown specification");
                return;
            }
            PathUtils.deleteRecursively(specDir);
            Files.deleteIfExists(specNameFile(segments.get(1)));
            http.writeJson(exchange, 200, Map.of("deleted", segments.get(1)));
            return;
        }
        http.writeError(exchange, 405, "Method not allowed");
    }

    /**
     * Accepts a zip archive of FHIR definition JSON files as the raw request body. The archive name may be
     * supplied through the {@code name} query parameter.
     */
    private void uploadSpec(HttpExchange exchange, String owner) throws IOException {
        Map<String, String> params = Http.queryParams(exchange);
        String specId = UUID.randomUUID().toString().substring(0, 12);
        Path specDir = config.getSpecsRoot().resolve(specId);
        Files.createDirectories(specDir);
        try (InputStream body = new BoundedInputStream(exchange.getRequestBody(), config.getMaxUploadBytes())) {
            int written = Zips.unzip(body, specDir);
            if (written == 0) {
                PathUtils.deleteRecursively(specDir);
                http.writeError(exchange, 400, "The uploaded archive did not contain any files. "
                        + "Upload a .zip of FHIR definition JSON files.");
                return;
            }
        } catch (BoundedInputStream.LimitExceededException e) {
            PathUtils.deleteRecursively(specDir);
            http.writeError(exchange, 413, "The uploaded archive exceeds the "
                    + (config.getMaxUploadBytes() / (1024 * 1024)) + " MB limit.");
            return;
        } catch (IOException e) {
            PathUtils.deleteRecursively(specDir);
            http.writeError(exchange, 400, "Unable to read the uploaded archive: " + describe(e));
            return;
        }

        // FHIR packages ship their resources under package/, and an archive this tool produced names that
        // folder after the project instead. Either way the definitions sit one level down, so a lone top-level
        // directory is flattened whatever it is called -- which also forgives a zip made by selecting a folder.
        Path nested = soleDirectory(specDir);
        if (nested != null) {
            flatten(nested, specDir);
        }

        Map<String, Object> spec = describeSpec(specId, params.get("name"));
        if (Integer.parseInt(String.valueOf(spec.get("definitionCount"))) == 0) {
            PathUtils.deleteRecursively(specDir);
            http.writeError(exchange, 400, "No FHIR StructureDefinition resources were found in the archive.");
            return;
        }
        // Kept beside the spec directory rather than inside it, so it never shows up in the file counts.
        Files.writeString(specNameFile(specId), params.get("name") == null ? specId : params.get("name"),
                StandardCharsets.UTF_8);
        workspaces.recordUploadOwner(specId, owner);
        http.writeJson(exchange, 201, spec);
    }

    /**
     * Reduces a name to something safe inside an archive, keeping the capitals someone typed.
     *
     * <p>Not {@code PathUtils.slug}, which lower-cases: that is right for a directory on this server, and
     * wrong for a folder the reader is going to look at, where "My US Core Profile" should not come back as
     * "my-us-core-profile".
     */
    private static String safeFolderName(String name) {
        String cleaned = name.trim().replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-").replaceAll("^[-.]+|[-.]+$", "");
        if (cleaned.isEmpty()) {
            return "package";
        }
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }

    /** The single directory an archive unpacked into, or null when its files are already at the top. */
    private static Path soleDirectory(Path root) throws IOException {
        try (var entries = Files.list(root)) {
            List<Path> children = entries.toList();
            List<Path> directories = children.stream().filter(Files::isDirectory).toList();
            boolean looseFiles = children.stream().anyMatch(Files::isRegularFile);
            return directories.size() == 1 && !looseFiles ? directories.get(0) : null;
        }
    }

    private void flatten(Path from, Path to) throws IOException {
        try (var entries = Files.list(from)) {
            for (Path entry : entries.toList()) {
                Path target = to.resolve(entry.getFileName().toString());
                if (!Files.exists(target)) {
                    Files.move(entry, target);
                }
            }
        }
        PathUtils.deleteRecursively(from);
    }

    /**
     * The archives this caller uploaded, plus any from before uploads had an owner.
     *
     * <p>The directory also holds a working copy per browser per package, so listing it unfiltered would show
     * everyone each other's work under a heading that means something else entirely.
     */
    private List<Map<String, Object>> listSpecs(String owner) throws IOException {
        Path root = config.getSpecsRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<Map<String, Object>> specs = new ArrayList<>();
        try (var entries = Files.list(root)) {
            for (Path entry : entries.filter(Files::isDirectory).toList()) {
                String specId = entry.getFileName().toString();
                if (!workspaces.isVisibleTo(specId, owner)) {
                    continue;
                }
                specs.add(describeSpec(specId, readSpecName(specId)));
            }
        }
        return specs;
    }

    private Path specNameFile(String specId) {
        return config.getSpecsRoot().resolve(specId + ".name");
    }

    private String readSpecName(String specId) {
        Path nameFile = specNameFile(specId);
        try {
            return Files.isRegularFile(nameFile) ? Files.readString(nameFile, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private Map<String, Object> describeSpec(String specId, String name) throws IOException {
        Path specDir = config.getSpecsRoot().resolve(specId);
        FileTrees.Summary summary = FileTrees.summarize(specDir);
        long definitions;
        try (var paths = Files.walk(specDir)) {
            definitions = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("StructureDefinition"))
                    .count();
        }
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("specId", specId);
        spec.put("name", name == null || name.isBlank() ? specId : name);
        spec.put("fileCount", summary.fileCount());
        spec.put("definitionCount", (int) definitions);
        spec.put("totalBytes", summary.totalBytes());
        return spec;
    }

    // ---------------------------------------------------------------- jobs

    private void handleJobs(HttpExchange exchange, String method, List<String> segments) throws Exception {
        if (segments.size() == 1) {
            switch (method) {
                case "GET" -> http.writeJson(exchange, 200, Map.of("jobs",
                        jobManager.list().stream().map(this::jobSummary).toList()));
                case "POST" -> createJob(exchange);
                default -> http.writeError(exchange, 405, "Method not allowed");
            }
            return;
        }

        String jobId = segments.get(1);
        Optional<Job> found = jobManager.find(jobId);
        if (found.isEmpty()) {
            http.writeError(exchange, 404, "Unknown job '" + jobId + "'");
            return;
        }
        Job job = found.get();

        if (segments.size() == 2) {
            switch (method) {
                case "GET" -> http.writeJson(exchange, 200, jobDetail(job));
                case "DELETE" -> http.writeJson(exchange, 200,
                        Map.of("deleted", jobManager.delete(jobId)));
                default -> http.writeError(exchange, 405, "Method not allowed");
            }
            return;
        }

        switch (segments.get(2)) {
            case "logs" -> requireGet(exchange, method, e -> {
                Map<String, String> params = Http.queryParams(e);
                Job.LogSlice slice = job.logsFrom(Http.intParam(params, "from", 0));
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("status", job.getStatus().name());
                body.put("from", slice.from());
                body.put("nextIndex", slice.nextIndex());
                body.put("lines", slice.lines());
                body.put("done", job.isTerminal());
                http.writeJson(e, 200, body);
            });
            case "tree" -> requireGet(exchange, method, e ->
                    http.writeJson(e, 200, Map.of(
                            "root", job.getOutputDir().getFileName().toString(),
                            "tree", FileTrees.build(job.getOutputDir()))));
            case "file" -> requireGet(exchange, method, e -> serveArtifact(e, job));
            case "archive" -> requireGet(exchange, method, e -> streamArchive(e, job));
            // Sent by the page when it is left. Deliberately a no-op unless the archive was downloaded first.
            case "collect" -> {
                if (!"POST".equals(method)) {
                    http.writeError(exchange, 405, "Method not allowed");
                } else {
                    jobManager.collect(job);
                    http.writeJson(exchange, 200, Map.of("collected", job.isCollected()));
                }
            }
            case "cancel" -> {
                if (!"POST".equals(method)) {
                    http.writeError(exchange, 405, "Method not allowed");
                    return;
                }
                http.writeJson(exchange, 200, Map.of("cancelled", jobManager.cancel(jobId)));
            }
            default -> http.writeError(exchange, 404, "Unknown job endpoint");
        }
    }

    private void createJob(HttpExchange exchange) throws Exception {
        GenerationRequest request = http.readJson(exchange, GenerationRequest.class);
        if (request == null) {
            throw new IllegalArgumentException("A generation request body is required.");
        }
        Job job = jobManager.submit(request);
        http.writeJson(exchange, 202, jobDetail(job));
    }

    private void serveArtifact(HttpExchange exchange, Job job) throws IOException {
        Map<String, String> params = Http.queryParams(exchange);
        String relative = params.get("path");
        if (relative == null || relative.isBlank()) {
            http.writeError(exchange, 400, "A 'path' query parameter is required.");
            return;
        }
        Path file;
        try {
            file = FileTrees.resolveSafely(job.getOutputDir(), relative);
        } catch (IllegalArgumentException e) {
            http.writeError(exchange, 400, e.getMessage());
            return;
        }
        if (!Files.isRegularFile(file)) {
            http.writeError(exchange, 404, "Unknown artifact '" + relative + "'");
            return;
        }
        long size = Files.size(file);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("path", relative);
        body.put("size", size);
        if (size > FileTrees.MAX_PREVIEW_BYTES) {
            body.put("truncated", true);
            body.put("content", null);
            body.put("message", "File is too large to preview. Download the archive instead.");
            http.writeJson(exchange, 200, body);
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        String content = decodeUtf8(bytes);
        body.put("truncated", false);
        body.put("binary", content == null);
        body.put("content", content);
        http.writeJson(exchange, 200, body);
    }

    /** Returns {@code null} when the bytes are not valid UTF-8 text. */
    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /**
     * Streams the generated artifacts and records that they have been handed over.
     *
     * <p>Nothing is deleted here. A stream that reached the end is not proof that what arrived is usable, and
     * the files are the only copy, so they stay until the page is left -- see the "collect" route. A transfer
     * that fails part way throws before the download is even recorded, leaving the job exactly as it was.
     */
    private void streamArchive(HttpExchange exchange, Job job) throws IOException {
        if (job.isCollected()) {
            http.writeError(exchange, 410, "This download has already been collected, and its files were "
                    + "removed from the server to make room. Generate again for a fresh copy.");
            return;
        }
        if (!Files.isDirectory(job.getOutputDir())) {
            http.writeError(exchange, 404, "This job has no artifacts.");
            return;
        }
        String fileName = job.getId() + ".zip";
        Map<String, String> headers = Map.of(
                "Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        try (OutputStream out = http.beginStream(exchange, 200, "application/zip", headers)) {
            Zips.zipDirectory(job.getOutputDir(), job.getId(), out);
        }
        job.setDownloadedAt(Instant.now());
    }

    private Map<String, Object> jobSummary(Job job) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", job.getId());
        summary.put("mode", job.getMode());
        summary.put("label", job.getLabel());
        summary.put("status", job.getStatus().name());
        summary.put("createdAt", job.getCreatedAt().toString());
        summary.put("startedAt", asString(job.getStartedAt()));
        summary.put("completedAt", asString(job.getCompletedAt()));
        summary.put("durationMillis", duration(job));
        summary.put("error", job.getErrorMessage());
        summary.put("stage", job.getStageKey());
        summary.put("stageTitle", job.getStageTitle());
        summary.put("progress", job.getProgressPercent());
        return summary;
    }

    private Map<String, Object> jobDetail(Job job) throws IOException {
        Map<String, Object> detail = new LinkedHashMap<>(jobSummary(job));
        detail.put("request", job.getRequest());
        // Names only. Enough for the page to print the exact command for this build, with no server paths.
        detail.put("facade", Map.of(
                "archiveRoot", job.getId(),
                "project", job.getFacadeProject() == null ? "" : job.getFacadeProject(),
                "jar", job.getFacadeJar() == null ? "" : job.getFacadeJar(),
                "port", 9090,
                "sampleResource", "Patient"));
        detail.put("downloaded", job.isDownloaded());
        detail.put("collected", job.isCollected());
        detail.put("collectedAt", job.getCollectedAt() == null ? null : job.getCollectedAt().toString());
        FileTrees.Summary summary = FileTrees.summarize(job.getOutputDir());
        detail.put("artifacts", Map.of(
                "fileCount", summary.fileCount(),
                "directoryCount", summary.directoryCount(),
                "totalBytes", summary.totalBytes()));
        return detail;
    }

    private static Long duration(Job job) {
        if (job.getStartedAt() == null) {
            return null;
        }
        Instant end = job.getCompletedAt() == null ? Instant.now() : job.getCompletedAt();
        return end.toEpochMilli() - job.getStartedAt().toEpochMilli();
    }

    private static String asString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private void requireGet(HttpExchange exchange, String method, ExchangeConsumer action) throws Exception {
        if (!"GET".equals(method)) {
            http.writeError(exchange, 405, "Method not allowed");
            return;
        }
        action.accept(exchange);
    }

    private static String describe(Throwable e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    /**
     * A route action that may fail.
     */
    private interface ExchangeConsumer {
        void accept(HttpExchange exchange) throws Exception;
    }
}
