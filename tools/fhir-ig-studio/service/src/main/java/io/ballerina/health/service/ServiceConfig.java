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

package io.ballerina.health.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Runtime configuration of the codegen service, resolved from system properties, environment variables and
 * defaults (in that order of precedence).
 */
public final class ServiceConfig {

    public static final String DEFAULT_WORKSPACE_DIR_NAME = ".fhir-tool-service";

    private final int port;
    private final String host;
    private final Path workspaceRoot;
    private final Path uiDir;
    private final int workerThreads;
    private final int jobTimeoutSeconds;
    private final long maxUploadBytes;
    private final String corsAllowOrigin;
    private final int workspaceTtlDays;
    private final String modelUrl;
    private final String modelName;
    private final String modelApiKey;
    private final String modelProvider;

    private ServiceConfig(int port, String host, Path workspaceRoot, Path uiDir, int workerThreads,
                          int jobTimeoutSeconds, long maxUploadBytes, String corsAllowOrigin,
                          int workspaceTtlDays, String modelUrl, String modelName, String modelApiKey,
                          String modelProvider) {
        this.port = port;
        this.host = host;
        this.workspaceRoot = workspaceRoot;
        this.uiDir = uiDir;
        this.workerThreads = workerThreads;
        this.jobTimeoutSeconds = jobTimeoutSeconds;
        this.maxUploadBytes = maxUploadBytes;
        this.corsAllowOrigin = corsAllowOrigin;
        this.workspaceTtlDays = workspaceTtlDays;
        this.modelUrl = modelUrl;
        this.modelName = modelName;
        this.modelApiKey = modelApiKey;
        this.modelProvider = modelProvider;
    }

    public static ServiceConfig load() {
        int port = intValue("fhir.tool.service.port", "FHIR_TOOL_SERVICE_PORT", 9095);
        String host = value("fhir.tool.service.host", "FHIR_TOOL_SERVICE_HOST", "0.0.0.0");
        Path workspace = pathValue("fhir.tool.service.workspace", "FHIR_TOOL_SERVICE_WORKSPACE",
                Paths.get(System.getProperty("user.home"), DEFAULT_WORKSPACE_DIR_NAME));
        Path uiDir = resolveUiDir();
        int workers = intValue("fhir.tool.service.workers", "FHIR_TOOL_SERVICE_WORKERS", 2);
        int timeout = intValue("fhir.tool.service.jobTimeoutSeconds", "FHIR_TOOL_SERVICE_JOB_TIMEOUT", 900);
        long maxUpload = intValue("fhir.tool.service.maxUploadMb", "FHIR_TOOL_SERVICE_MAX_UPLOAD_MB", 200)
                * 1024L * 1024L;
        String cors = value("fhir.tool.service.corsAllowOrigin", "FHIR_TOOL_SERVICE_CORS_ORIGIN", "*");
        int workspaceTtlDays = intValue("fhir.tool.service.workspaceTtlDays",
                "FHIR_TOOL_SERVICE_WORKSPACE_TTL_DAYS", 7);
        // Empty by default: the chat is an optional extra, and the tool is wholly usable without it. Guessing
        // at a local address instead meant a first run reported a model that was "not answering", which reads
        // as something broken rather than something not switched on.
        String modelUrl = value("fhir.tool.service.modelUrl", "FHIR_TOOL_SERVICE_MODEL_URL", "");
        // A code-trained model, because what is asked of it is a strict JSON block as much as prose. Only a
        // default name: it does nothing until an address is configured alongside it.
        String modelName = value("fhir.tool.service.model", "FHIR_TOOL_SERVICE_MODEL", "qwen2.5-coder:7b");
        String modelKey = value("fhir.tool.service.modelApiKey", "FHIR_TOOL_SERVICE_MODEL_API_KEY", "");
        // openai (the default, and what almost everything speaks), azure, or anthropic.
        String modelProvider = value("fhir.tool.service.modelProvider", "FHIR_TOOL_SERVICE_MODEL_PROVIDER", "");
        return new ServiceConfig(port, host, workspace, uiDir, Math.max(1, workers), timeout, maxUpload, cors,
                workspaceTtlDays, modelUrl, modelName, modelKey, modelProvider);
    }

    /**
     * Locates the built UI assets. Explicit configuration wins; otherwise the well known locations relative to
     * the service jar and the working directory are probed so that {@code java -jar} works from a source checkout.
     */
    private static Path resolveUiDir() {
        String configured = value("fhir.tool.service.uiDir", "FHIR_TOOL_SERVICE_UI_DIR", null);
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }
        // "../ui/dist" covers running from the service directory, whose sibling is the ui/ project:
        //   tools/fhir-ig-studio/{service,ui}
        Path[] candidates = {
                Paths.get("ui", "dist"),
                Paths.get("..", "ui", "dist"),
                Paths.get("target", "ui"),
        };
        for (Path candidate : candidates) {
            Path resolved = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(resolved.resolve("index.html"))) {
                return resolved;
            }
        }
        return Paths.get("ui", "dist").toAbsolutePath().normalize();
    }

    /**
     * Settings for this installation, read once from a file so they outlive a restart.
     *
     * <p>Everything here can be set with a system property or an environment variable, which is right for a
     * container and wrong for a machine someone actually works on: the service gets restarted, and the export
     * lines are gone. A plain properties file alongside the workspace keeps a choice like which model to use
     * where it was made.
     *
     * <p>It is the weakest of the three sources. An environment variable still wins, so a deployment can
     * override what a developer left behind.
     */
    private static final Properties FILE_SETTINGS = loadFileSettings();

    private static Properties loadFileSettings() {
        Properties settings = new Properties();
        String configured = System.getProperty("fhir.tool.service.configFile");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("FHIR_TOOL_SERVICE_CONFIG");
        }
        Path file = configured != null && !configured.isBlank()
                ? Paths.get(configured)
                : Paths.get(System.getProperty("user.home"), DEFAULT_WORKSPACE_DIR_NAME, "service.properties");
        if (!Files.isRegularFile(file)) {
            return settings;
        }
        try (var in = Files.newInputStream(file)) {
            settings.load(in);
        } catch (IOException e) {
            System.err.println("[WARN] Could not read " + file + ": " + e.getMessage());
        }
        return settings;
    }

    /**
     * A setting, from whichever source has it: a system property, then the environment, then the file.
     *
     * <p>The file is keyed by either name, so {@code fhir.tool.service.model} and
     * {@code FHIR_TOOL_SERVICE_MODEL} both work and nobody has to remember which form a file wants.
     */
    private static String value(String systemProperty, String envVar, String defaultValue) {
        String fromProperty = System.getProperty(systemProperty);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromFile = FILE_SETTINGS.getProperty(systemProperty);
        if (fromFile == null || fromFile.isBlank()) {
            fromFile = FILE_SETTINGS.getProperty(envVar);
        }
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return defaultValue;
    }

    private static int intValue(String systemProperty, String envVar, int defaultValue) {
        String raw = value(systemProperty, envVar, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Path pathValue(String systemProperty, String envVar, Path defaultValue) {
        String raw = value(systemProperty, envVar, null);
        return raw == null ? defaultValue : Paths.get(raw).toAbsolutePath().normalize();
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    public Path getSpecsRoot() {
        return workspaceRoot.resolve("specs");
    }

    public Path getJobsRoot() {
        return workspaceRoot.resolve("jobs");
    }

    public Path getIgCacheDir() {
        return workspaceRoot.resolve(".fhir-ig-cache");
    }

    public Path getUiDir() {
        return uiDir;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public int getJobTimeoutSeconds() {
        return jobTimeoutSeconds;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public String getCorsAllowOrigin() {
        return corsAllowOrigin;
    }

    /**
     * How long a browser's working copy of a package is kept after it was last used. Each browser gets its own,
     * so without a cut-off the disk only ever grows. Zero or less turns the sweep off.
     */
    public int getWorkspaceTtlDays() {
        return workspaceTtlDays;
    }

    /** Base URL of the chat API, without the trailing /chat/completions. Empty when the chat is not enabled. */
    public String getModelUrl() {
        return modelUrl;
    }

    /**
     * Whether the chat has been switched on.
     *
     * <p>An address is the whole of it: a model name alone means nothing, and a key is only wanted by a hosted
     * provider. Everything else about the tool works whether this is true or not.
     */
    public boolean isChatConfigured() {
        return modelUrl != null && !modelUrl.isBlank();
    }

    public String getModelName() {
        return modelName;
    }

    /**
     * Empty for a model on this machine; required by a hosted one.
     *
     * <p>Never returned by the API and never logged: it is read here and presented to the model provider, and
     * nowhere else. Set it from the environment rather than a command line, where it would be visible to
     * anything that can list processes.
     */
    public String getModelApiKey() {
        return modelApiKey;
    }

    /**
     * Which chat API is being spoken: {@code openai}, {@code azure} or {@code anthropic}.
     *
     * <p>Blank means work it out from the address, so the common cases need no setting at all.
     */
    public String getModelProvider() {
        if (modelProvider != null && !modelProvider.isBlank()) {
            return modelProvider;
        }
        String url = modelUrl == null ? "" : modelUrl.toLowerCase(java.util.Locale.ROOT);
        if (url.contains("anthropic.com")) {
            return "anthropic";
        }
        if (url.contains("openai.azure.com") || url.contains("cognitiveservices")) {
            return "azure";
        }
        return "openai";
    }
}
