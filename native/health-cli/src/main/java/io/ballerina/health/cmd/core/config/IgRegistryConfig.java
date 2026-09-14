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

package io.ballerina.health.cmd.core.config;

import com.google.gson.JsonObject;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for FHIR IG package registry download ({@code fhir.igRegistry} in tool-config.json).
 */
public class IgRegistryConfig {

    private final String registryUrl;
    private final String cacheDir;
    private final int httpTimeoutSeconds;
    private final Map<String, IgPackageRef> defaultPackages;
    private final Map<String, String> packageMappings;

    public IgRegistryConfig(String registryUrl, String cacheDir, int httpTimeoutSeconds,
                            Map<String, IgPackageRef> defaultPackages, Map<String, String> packageMappings) {
        this.registryUrl = registryUrl;
        this.cacheDir = cacheDir;
        this.httpTimeoutSeconds = httpTimeoutSeconds;
        this.defaultPackages = defaultPackages != null ? defaultPackages : Collections.emptyMap();
        this.packageMappings = packageMappings != null ? packageMappings : Collections.emptyMap();
    }

    public static IgRegistryConfig fromFhirConfig(JsonObject fhirConfig) {
        if (fhirConfig == null || !fhirConfig.has("igRegistry")) {
            return defaults();
        }
        JsonObject igRegistry = fhirConfig.getAsJsonObject("igRegistry");
        String registryUrl = getString(igRegistry, "registryUrl", HealthCmdConstants.CMD_DEFAULT_REGISTRY_URL);
        String cacheDir = getString(igRegistry, "cacheDir", HealthCmdConstants.CMD_DEFAULT_IG_CACHE_DIR);
        int timeout = igRegistry.has("httpTimeoutSeconds")
                ? igRegistry.get("httpTimeoutSeconds").getAsInt()
                : HealthCmdConstants.CMD_DEFAULT_IG_HTTP_TIMEOUT_SECONDS;

        Map<String, IgPackageRef> defaultPackages = new HashMap<>();
        if (igRegistry.has("defaultPackages")) {
            JsonObject defaults = igRegistry.getAsJsonObject("defaultPackages");
            for (String fhirVersion : defaults.keySet()) {
                JsonObject pkg = defaults.getAsJsonObject(fhirVersion);
                if (pkg == null || !pkg.has("name") || !pkg.has("version")) {
                    continue;
                }
                defaultPackages.put(fhirVersion, new IgPackageRef(
                        pkg.get("name").getAsString(),
                        pkg.get("version").getAsString()
                ));
            }
        }
        // Ensure the r4/r5 fallbacks used by getDefaultPackage() are always present, even when the
        // configured "defaultPackages" is malformed or only overrides one of the two FHIR versions.
        defaultPackages.putIfAbsent("r4", new IgPackageRef(
                HealthCmdConstants.CMD_DEFAULT_R4_IG_PACKAGE_NAME,
                HealthCmdConstants.CMD_DEFAULT_R4_IG_PACKAGE_VERSION));
        defaultPackages.putIfAbsent("r5", new IgPackageRef(
                HealthCmdConstants.CMD_DEFAULT_R5_IG_PACKAGE_NAME,
                HealthCmdConstants.CMD_DEFAULT_R5_IG_PACKAGE_VERSION));

        Map<String, String> mappings = new HashMap<>();
        if (igRegistry.has("packageMappings")) {
            JsonObject mappingObj = igRegistry.getAsJsonObject("packageMappings");
            for (String key : mappingObj.keySet()) {
                mappings.put(key, mappingObj.get(key).getAsString());
            }
        }

        return new IgRegistryConfig(registryUrl, cacheDir, timeout, defaultPackages, mappings);
    }

    public static IgRegistryConfig defaults() {
        Map<String, IgPackageRef> defaultPackages = Map.of(
                "r4", new IgPackageRef(
                        HealthCmdConstants.CMD_DEFAULT_R4_IG_PACKAGE_NAME,
                        HealthCmdConstants.CMD_DEFAULT_R4_IG_PACKAGE_VERSION),
                "r5", new IgPackageRef(
                        HealthCmdConstants.CMD_DEFAULT_R5_IG_PACKAGE_NAME,
                        HealthCmdConstants.CMD_DEFAULT_R5_IG_PACKAGE_VERSION)
        );
        return new IgRegistryConfig(
                HealthCmdConstants.CMD_DEFAULT_REGISTRY_URL,
                HealthCmdConstants.CMD_DEFAULT_IG_CACHE_DIR,
                HealthCmdConstants.CMD_DEFAULT_IG_HTTP_TIMEOUT_SECONDS,
                defaultPackages,
                Collections.emptyMap()
        );
    }

    private static String getString(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).getAsString().isEmpty()) {
            return obj.get(key).getAsString();
        }
        return defaultValue;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public int getHttpTimeoutSeconds() {
        return httpTimeoutSeconds;
    }

    public IgPackageRef getDefaultPackage(String fhirVersion) {
        if (fhirVersion == null) {
            return defaultPackages.get("r4");
        }
        return defaultPackages.getOrDefault(fhirVersion.toLowerCase(), defaultPackages.get("r4"));
    }

    /**
     * Looks up a known published Ballerina package for an exact {@code <igName>@<igVersion>} match. Keyed by the
     * full pair (not just the IG name) since a mapped package targets one specific IG version -- the package's
     * own version-ish naming (e.g. "carinbb200") does not reliably indicate which IG version it was built from.
     */
    public String resolveDependentPackage(String igName, String igVersion) {
        if (igName == null || igVersion == null) {
            return null;
        }
        return packageMappings.get(igName + "@" + igVersion);
    }

    public record IgPackageRef(String name, String version) {
    }
}
