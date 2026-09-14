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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Resolves FHIR specification directories for template generation, including the
 * international base R4 structure-definition layout documented under spec-path/international.
 */
public final class SpecificationPathUtils {

    private static final String INTERNATIONAL_IG_DIR = "international";
    private static final String STRUCTURE_DEFINITION_PREFIX = "StructureDefinition";
    private static final String HL7_BASE_STRUCTURE_DEFINITION_URL_PREFIX =
            "http://hl7.org/fhir/StructureDefinition/";

    private SpecificationPathUtils() {
    }

    /**
     * Resolves the directory that contains FHIR structure definitions for template generation.
     * When the given path is a spec root (with IG subfolders), the international subdirectory is used.
     * When no path is given, {@code <executionPath>/spec/international} is used.
     */
    public static Path resolveTemplateSpecificationPath(String specPathParam, String executionPath)
            throws BallerinaHealthException {
        if (specPathParam != null && !specPathParam.trim().isEmpty()) {
            Path specificationPath = HealthCmdUtils.getSpecificationPath(specPathParam, executionPath);
            if (!Files.isDirectory(specificationPath)) {
                throw new BallerinaHealthException("Cannot find valid spec path pointed. Please check the path "
                        + specPathParam + " is valid.");
            }
            if (containsStructureDefinitionResources(specificationPath)) {
                return specificationPath;
            }
            Path internationalPath = specificationPath.resolve(INTERNATIONAL_IG_DIR);
            if (Files.isDirectory(internationalPath) && containsStructureDefinitionResources(internationalPath)) {
                return internationalPath;
            }
            throw new BallerinaHealthException("No FHIR StructureDefinition resources found under "
                    + specificationPath + ". Point to an IG folder or to a spec root that contains an '"
                    + INTERNATIONAL_IG_DIR + "' directory with HL7 international base R4 definitions.");
        }

        Path defaultInternationalPath = Paths.get(executionPath, "spec", INTERNATIONAL_IG_DIR);
        if (Files.isDirectory(defaultInternationalPath) && containsStructureDefinitionResources(defaultInternationalPath)) {
            return defaultInternationalPath;
        }
        throw new BallerinaHealthException("No IG specification path provided. Either pass the path to FHIR "
                + "definitions as the last argument, or place HL7 international base R4 StructureDefinition "
                + "files under spec/international relative to the working directory.");
    }

    /**
     * Returns true when the specification path only contains HL7 international base structure definitions
     * (no regional or custom implementation guide profiles).
     */
    public static boolean isInternationalBaseSpecification(String specificationPath) throws IOException {
        Path specPath = Path.of(specificationPath);
        if (specPath.getFileName() != null
                && INTERNATIONAL_IG_DIR.equalsIgnoreCase(specPath.getFileName().toString())) {
            return true;
        }
        if (hasCustomImplementationGuide(specPath)) {
            return false;
        }
        return containsOnlyHl7BaseStructureDefinitions(specPath);
    }

    /**
     * Reads the {@code version} field from {@code directory/package.json} (the FHIR NPM package manifest),
     * or {@code null} when the manifest or its version field is missing/unreadable.
     */
    public static String readInstalledPackageVersion(Path directory) {
        Path packageJson = directory.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            return null;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(packageJson)).getAsJsonObject();
            if (!json.has("version") || json.get("version").getAsString().trim().isEmpty()) {
                return null;
            }
            return json.get("version").getAsString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean containsStructureDefinitionResources(Path directory) throws BallerinaHealthException {
        try {
            if (!Files.isDirectory(directory)) {
                return false;
            }
            try (Stream<Path> paths = Files.walk(directory, 2)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith(STRUCTURE_DEFINITION_PREFIX))
                        .anyMatch(SpecificationPathUtils::isStructureDefinitionFile);
            }
        } catch (IOException e) {
            throw new BallerinaHealthException("Unable to read specification path: " + directory, e);
        }
    }

    private static boolean hasCustomImplementationGuide(Path specificationPath) throws IOException {
        try (Stream<Path> paths = Files.walk(specificationPath, 2)) {
            List<Path> implementationGuideFiles = paths
                    .filter(Files::isRegularFile)
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
                    if (json.has("packageId") && !json.get("packageId").getAsString().isEmpty()) {
                        return true;
                    }
                    if (json.has("name") && !json.get("name").getAsString().isEmpty()
                            && !"FHIR".equalsIgnoreCase(json.get("name").getAsString())) {
                        return true;
                    }
                } catch (Exception ignored) {
                    // try next candidate file
                }
            }
        }
        return false;
    }

    private static boolean containsOnlyHl7BaseStructureDefinitions(Path specificationPath) throws IOException {
        boolean foundStructureDefinition = false;
        try (Stream<Path> paths = Files.walk(specificationPath, 2)) {
            List<Path> structureDefinitionFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(STRUCTURE_DEFINITION_PREFIX))
                    .toList();

            for (Path path : structureDefinitionFiles) {
                if (!isStructureDefinitionFile(path)) {
                    continue;
                }
                foundStructureDefinition = true;
                try {
                    JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    if (!json.has("url")) {
                        return false;
                    }
                    String url = json.get("url").getAsString();
                    if (!url.startsWith(HL7_BASE_STRUCTURE_DEFINITION_URL_PREFIX)) {
                        return false;
                    }
                    String resourceName = url.substring(HL7_BASE_STRUCTURE_DEFINITION_URL_PREFIX.length());
                    if (resourceName.contains("/")) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return foundStructureDefinition;
    }

    private static boolean isStructureDefinitionFile(Path path) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            return json.has("resourceType")
                    && "StructureDefinition".equals(json.get("resourceType").getAsString());
        } catch (Exception e) {
            return false;
        }
    }

}
