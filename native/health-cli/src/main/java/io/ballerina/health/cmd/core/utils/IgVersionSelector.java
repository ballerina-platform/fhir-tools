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

import java.io.Console;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses FHIR package registry metadata and interactively selects a package version when needed.
 */
public final class IgVersionSelector {

    private IgVersionSelector() {
    }

    /**
     * Returns published version ids from registry metadata, sorted newest-first (approximate semver ordering).
     */
    public static List<String> parseAvailableVersions(String metadataJson) throws BallerinaHealthException {
        try {
            JsonObject root = JsonParser.parseString(metadataJson).getAsJsonObject();
            if (!root.has("versions") || !root.get("versions").isJsonObject()) {
                throw new BallerinaHealthException("Package metadata does not contain a versions map.");
            }
            Set<Map.Entry<String, com.google.gson.JsonElement>> entries =
                    root.getAsJsonObject("versions").entrySet();
            List<String> versions = new ArrayList<>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : entries) {
                versions.add(entry.getKey());
            }
            versions.sort(IgVersionSelector::compareVersionsDesc);
            return versions;
        } catch (BallerinaHealthException e) {
            throw e;
        } catch (Exception e) {
            throw new BallerinaHealthException("Failed to parse available versions from package metadata: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Prompts the user to choose a version. When no console is available or {@code nonInteractive} is true,
     * returns {@code latestTagVersion} without prompting.
     */
    public static String selectVersion(String packageName, List<String> availableVersions, String latestTagVersion,
                                       boolean nonInteractive, PrintStream out) throws BallerinaHealthException {
        if (availableVersions == null || availableVersions.isEmpty()) {
            if (latestTagVersion != null && !latestTagVersion.isEmpty()) {
                return latestTagVersion;
            }
            throw new BallerinaHealthException("No published versions found for package " + packageName + ".");
        }

        if (nonInteractive || !isInteractiveConsoleAvailable()) {
            if (latestTagVersion != null && availableVersions.contains(latestTagVersion)) {
                return latestTagVersion;
            }
            return availableVersions.get(0);
        }

        PrintStream output = out != null ? out : System.out;
        int defaultIndex = resolveDefaultIndex(availableVersions, latestTagVersion);
        output.println("[INFO] Available versions for " + packageName + ":");
        for (int i = 0; i < availableVersions.size(); i++) {
            String suffix = (i == defaultIndex) ? " (default)" : "";
            output.printf("  [%d] %s%s%n", i + 1, availableVersions.get(i), suffix);
        }

        Console console = System.console();
        while (true) {
            String prompt = "Select version [1-" + availableVersions.size() + "] (default "
                    + (defaultIndex + 1) + "): ";
            String input = console.readLine(prompt);
            if (input == null || input.trim().isEmpty()) {
                return availableVersions.get(defaultIndex);
            }
            String trimmed = input.trim();
            if (trimmed.matches("\\d+")) {
                int choice = Integer.parseInt(trimmed);
                if (choice >= 1 && choice <= availableVersions.size()) {
                    return availableVersions.get(choice - 1);
                }
                output.println("[ERROR] Invalid selection. Enter a number between 1 and "
                        + availableVersions.size() + ".");
                continue;
            }
            if (availableVersions.contains(trimmed)) {
                return trimmed;
            }
            output.println("[ERROR] Unknown version '" + trimmed + "'. Choose from the list above.");
        }
    }

    static boolean isInteractiveConsoleAvailable() {
        return System.console() != null;
    }

    private static int resolveDefaultIndex(List<String> availableVersions, String latestTagVersion) {
        if (latestTagVersion != null) {
            int index = availableVersions.indexOf(latestTagVersion);
            if (index >= 0) {
                return index;
            }
        }
        return 0;
    }

    static int compareVersionsDesc(String left, String right) {
        return compareVersions(right, left);
    }

    static int compareVersions(String left, String right) {
        VersionParts leftParts = splitVersionParts(left);
        VersionParts rightParts = splitVersionParts(right);
        int baseCompare = compareBaseVersion(leftParts.base(), rightParts.base());
        if (baseCompare != 0) {
            return baseCompare;
        }
        return comparePrerelease(leftParts.prerelease(), rightParts.prerelease());
    }

    private static VersionParts splitVersionParts(String version) {
        int dash = version.indexOf('-');
        if (dash < 0) {
            return new VersionParts(version, "");
        }
        return new VersionParts(version.substring(0, dash), version.substring(dash + 1));
    }

    private static int compareBaseVersion(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int max = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < max; i++) {
            String l = i < leftParts.length ? leftParts[i] : "";
            String r = i < rightParts.length ? rightParts[i] : "";
            int partCompare = compareVersionPart(l, r);
            if (partCompare != 0) {
                return partCompare;
            }
        }
        return 0;
    }

    private static int comparePrerelease(String left, String right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 0;
        }
        if (left.isEmpty()) {
            return 1;
        }
        if (right.isEmpty()) {
            return -1;
        }
        return left.compareToIgnoreCase(right);
    }

    private record VersionParts(String base, String prerelease) {
    }

    private static int compareVersionPart(String left, String right) {
        boolean leftNumeric = left.matches("\\d+");
        boolean rightNumeric = right.matches("\\d+");
        if (leftNumeric && rightNumeric) {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        }
        return left.compareToIgnoreCase(right);
    }
}
