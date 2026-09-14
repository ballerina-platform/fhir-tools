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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgVersionSelectorTest {

    private static final String US_CORE_METADATA = """
            {
              "name": "hl7.fhir.us.core",
              "dist-tags": { "latest": "9.0.0" },
              "versions": {
                "6.1.0": { "fhirVersion": "4.0.1" },
                "9.0.0": { "fhirVersion": "4.0.1" },
                "9.0.0-ballot": { "fhirVersion": "4.0.1" }
              }
            }
            """;

    @Test
    void parseAvailableVersionsReturnsNewestFirst() throws Exception {
        List<String> versions = IgVersionSelector.parseAvailableVersions(US_CORE_METADATA);
        assertEquals(3, versions.size());
        assertEquals("9.0.0", versions.get(0));
        assertTrue(versions.contains("6.1.0"));
    }

    @Test
    void selectVersionNonInteractiveUsesLatestTag() throws Exception {
        List<String> versions = IgVersionSelector.parseAvailableVersions(US_CORE_METADATA);
        String selected = IgVersionSelector.selectVersion(
                "hl7.fhir.us.core", versions, "9.0.0", true, null);
        assertEquals("9.0.0", selected);
    }

    @Test
    void selectVersionNonInteractiveFallsBackToFirstWhenLatestMissing() throws Exception {
        List<String> versions = List.of("6.1.0", "5.0.0");
        String selected = IgVersionSelector.selectVersion(
                "hl7.fhir.us.core", versions, null, true, null);
        assertEquals("6.1.0", selected);
    }

    @Test
    void compareVersionsOrdersNumericSegments() {
        assertTrue(IgVersionSelector.compareVersionsDesc("9.0.0", "6.1.0") < 0);
        assertTrue(IgVersionSelector.compareVersionsDesc("9.0.0-ballot", "9.0.0") > 0);
    }

    @Test
    void selectVersionNonInteractiveDoesNotWritePrompt() throws Exception {
        List<String> versions = IgVersionSelector.parseAvailableVersions(US_CORE_METADATA);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(buffer);
        IgVersionSelector.selectVersion("hl7.fhir.us.core", versions, "9.0.0", true, capture);
        assertEquals(0, buffer.size());
    }

    @Test
    void selectVersionFallsBackToLatestTagWhenNoInteractiveConsole() throws Exception {
        // On JDK 22-24, System.console() can report non-null even with JUnit's redirected streams (JDK-8309155),
        // so asserting isInteractiveConsoleAvailable() directly is JDK-version-dependent. Skip in that case
        // rather than assert environment-specific console behavior; otherwise, verify the actual
        // selection behavior that matters: with no interactive console, selectVersion() must not block on
        // input and must fall back to the latest-tag version automatically, exactly like the explicit
        // nonInteractive=true path.
        Assumptions.assumeTrue(!IgVersionSelector.isInteractiveConsoleAvailable(),
                "Skipping: this JDK reports an interactive console even under redirected test I/O");
        List<String> versions = IgVersionSelector.parseAvailableVersions(US_CORE_METADATA);
        String selected = IgVersionSelector.selectVersion(
                "hl7.fhir.us.core", versions, "9.0.0", false, null);
        assertEquals("9.0.0", selected);
    }
}
