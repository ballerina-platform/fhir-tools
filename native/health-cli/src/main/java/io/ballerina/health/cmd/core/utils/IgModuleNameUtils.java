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
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utilities for deriving Ballerina module names from FHIR implementation guide identifiers.
 */
public final class IgModuleNameUtils {

    private static final String MODULE_NAME_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]*$";

    private IgModuleNameUtils() {
    }

    public static boolean isValidBallerinaModuleName(String moduleName) {
        return moduleName != null && !moduleName.isEmpty() && moduleName.matches(MODULE_NAME_PATTERN);
    }

    public static String inferIgModuleName(String specificationPath) throws IOException {
        String igIdentifier = findImplementationGuideIdentifier(specificationPath);
        if (igIdentifier == null || igIdentifier.isEmpty()) {
            igIdentifier = inferIgNameFromDirectory(specificationPath);
        }
        return toBallerinaModuleName(igIdentifier);
    }

    public static String toBallerinaModuleName(String igIdentifier) {
        if (igIdentifier == null || igIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("IG identifier cannot be empty");
        }
        String normalized = igIdentifier.trim().replace('.', '_');
        normalized = GeneratorUtils.getInstance().resolveSpecialCharacters(normalized);
        normalized = camelCaseToSnakeCase(normalized);
        normalized = normalized.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("_+", "_");
        if (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Unable to derive a Ballerina module name from IG identifier: "
                    + igIdentifier);
        }
        if (!Character.isLetter(normalized.charAt(0))) {
            normalized = "ig_" + normalized;
        }
        return normalized;
    }

    private static String camelCaseToSnakeCase(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current) && i > 0 && builder.charAt(builder.length() - 1) != '_') {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(current));
        }
        return builder.toString();
    }

    private static String findImplementationGuideIdentifier(String specificationPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(specificationPath))) {
            List<Path> implementationGuideFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> path.getFileName().toString().startsWith("ImplementationGuide")
                            || path.getFileName().toString().contains("ImplementationGuide"))
                    .toList();

            for (Path path : implementationGuideFiles) {
                try {
                    JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    if (!json.has("resourceType")
                            || !"ImplementationGuide".equals(json.get("resourceType").getAsString())) {
                        continue;
                    }
                    if (json.has("name") && !json.get("name").getAsString().isEmpty()) {
                        return json.get("name").getAsString();
                    }
                    if (json.has("packageId") && !json.get("packageId").getAsString().isEmpty()) {
                        return json.get("packageId").getAsString();
                    }
                    if (json.has("id") && !json.get("id").getAsString().isEmpty()) {
                        return json.get("id").getAsString();
                    }
                } catch (Exception ignored) {
                    // try next candidate file
                }
            }
        }
        return null;
    }

    private static String inferIgNameFromDirectory(String specificationPath) {
        Path specPath = Paths.get(specificationPath);
        String directoryName = specPath.getFileName().toString();
        int separatorIndex = directoryName.lastIndexOf('.');
        if (separatorIndex >= 0 && separatorIndex < directoryName.length() - 1) {
            return directoryName.substring(separatorIndex + 1);
        }
        return directoryName;
    }
}
