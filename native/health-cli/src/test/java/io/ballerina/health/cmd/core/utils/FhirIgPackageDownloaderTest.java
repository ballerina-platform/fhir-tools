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

import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FhirIgPackageDownloaderTest {

    @TempDir
    Path tempDir;

    @Test
    void extractPackageWritesStructureDefinitionJson() throws Exception {
        byte[] tgz = buildSamplePackageTgz();
        Path target = tempDir.resolve("extracted");
        FhirIgPackageDownloader.extractPackage(tgz, target);

        Path patientSd = target.resolve("StructureDefinition-Patient.json");
        assertTrue(Files.exists(patientSd));
        String content = Files.readString(patientSd);
        assertTrue(content.contains("\"resourceType\": \"StructureDefinition\""));
        assertTrue(SpecificationPathUtils.containsStructureDefinitionResources(target));
    }

    @Test
    void downloadAndExtractSkipsWhenDefinitionsPresent() throws Exception {
        Path target = tempDir.resolve("cached-ig");
        Files.createDirectories(target);
        Files.writeString(target.resolve("StructureDefinition-Patient.json"), """
                {
                  "resourceType": "StructureDefinition",
                  "url": "http://hl7.org/fhir/StructureDefinition/Patient",
                  "name": "Patient",
                  "type": "Patient",
                  "kind": "resource",
                  "fhirVersion": "4.0.1"
                }
                """);

        FhirIgPackageDownloader.DownloadOptions options = new FhirIgPackageDownloader.DownloadOptions(
                "https://packages.fhir.org", tempDir.resolve("cache").toString(), 60, false);
        Path result = FhirIgPackageDownloader.downloadAndExtract(
                target, "hl7.fhir.r4.core", "4.0.1", options);
        assertEquals(target, result);
    }

    @Test
    void parseIgSpecUsesLatestSentinelWhenVersionMissing() {
        FhirIgPackageDownloader.ParsedIgSpec spec =
                FhirIgPackageDownloader.parseIgSpec("hl7.fhir.us.core", null);
        assertEquals("hl7.fhir.us.core", spec.name());
        assertEquals("latest", spec.version());
    }

    @Test
    void parseLatestVersionFromMetadata() throws Exception {
        String metadata = """
                {
                  "name": "hl7.fhir.us.core",
                  "dist-tags": { "latest": "9.0.0" }
                }
                """;
        assertEquals("9.0.0",
                FhirIgPackageDownloader.parseLatestVersionFromMetadata(metadata, "hl7.fhir.us.core"));
    }

    @Test
    void parseLatestVersionFromMetadataMissingDistTags() {
        assertThrows(BallerinaHealthException.class, () ->
                FhirIgPackageDownloader.parseLatestVersionFromMetadata("{\"name\":\"x\"}", "x"));
    }

    @Test
    void resolvePackageVersionKeepsExplicitVersion() throws Exception {
        FhirIgPackageDownloader.DownloadOptions options = new FhirIgPackageDownloader.DownloadOptions(
                "https://packages.fhir.org", tempDir.resolve("cache").toString(), 60, false, true, false, true,
                null);
        assertEquals("6.1.0", FhirIgPackageDownloader.resolvePackageVersion(
                "https://packages.fhir.org", "hl7.fhir.us.core", "6.1.0", options));
    }

    @Test
    void sanitizeIgDirectoryName() {
        assertEquals("hl7_fhir_us_core", FhirIgPackageDownloader.sanitizeIgDirectoryName("hl7.fhir.us.core"));
    }

    @Test
    void downloadAndExtractRefetchesWhenInstalledVersionDiffers() throws Exception {
        Path target = tempDir.resolve("stale-ig");
        Files.createDirectories(target);
        Files.writeString(target.resolve("package.json"), "{\"name\":\"hl7.fhir.us.core\",\"version\":\"6.1.0\"}");
        Files.writeString(target.resolve("StructureDefinition-Patient.json"), """
                {
                  "resourceType": "StructureDefinition",
                  "url": "http://hl7.org/fhir/StructureDefinition/Patient",
                  "name": "StalePatient"
                }
                """);

        Path cacheDir = tempDir.resolve("mismatch-cache");
        Files.createDirectories(cacheDir);
        Files.write(cacheDir.resolve("hl7.fhir.us.core-9.0.0.tgz"), buildSamplePackageTgz());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FhirIgPackageDownloader.DownloadOptions options = new FhirIgPackageDownloader.DownloadOptions(
                "https://packages.fhir.org", cacheDir.toString(), 60, false, false, false, true,
                new PrintStream(buffer));

        Path result = FhirIgPackageDownloader.downloadAndExtract(target, "hl7.fhir.us.core", "9.0.0", options);

        assertEquals(target, result);
        String content = Files.readString(target.resolve("StructureDefinition-Patient.json"));
        assertTrue(content.contains("\"name\": \"Patient\""));
        assertTrue(!content.contains("StalePatient"));
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("[WARN] Replacing existing IG definitions"));
    }

    @Test
    void parseIgReferenceSplitsNameAndVersion() {
        FhirIgPackageDownloader.ParsedIgSpec spec =
                FhirIgPackageDownloader.parseIgReference("hl7.fhir.us.core@8.0.1");
        assertEquals("hl7.fhir.us.core", spec.name());
        assertEquals("8.0.1", spec.version());
    }

    @Test
    void parseIgReferenceWithoutVersionUsesLatestSentinel() {
        FhirIgPackageDownloader.ParsedIgSpec spec =
                FhirIgPackageDownloader.parseIgReference("hl7.fhir.us.core");
        assertEquals("hl7.fhir.us.core", spec.name());
        assertEquals("latest", spec.version());
    }

    @Test
    void parseIgReferenceSplitsOnLastAtSign() {
        FhirIgPackageDownloader.ParsedIgSpec spec = FhirIgPackageDownloader.parseIgReference("foo@bar@1.0.0");
        assertEquals("foo@bar", spec.name());
        assertEquals("1.0.0", spec.version());
    }

    @Test
    void isValidIgReferenceRejectsMissingName() {
        assertTrue(!FhirIgPackageDownloader.isValidIgReference("@8.0.1"));
        assertTrue(!FhirIgPackageDownloader.isValidIgReference(""));
        assertTrue(!FhirIgPackageDownloader.isValidIgReference(null));
    }

    @Test
    void isValidIgReferenceAcceptsBareNameOrNameAtVersion() {
        assertTrue(FhirIgPackageDownloader.isValidIgReference("hl7.fhir.us.core"));
        assertTrue(FhirIgPackageDownloader.isValidIgReference("hl7.fhir.us.core@8.0.1"));
    }

    @Test
    void fetchesFromCacheWithoutNetworkOrWarningWhenPreSeeded() throws Exception {
        byte[] tgz = buildSamplePackageTgz();
        Path cacheDir = tempDir.resolve("cache");
        Files.createDirectories(cacheDir);
        Files.write(cacheDir.resolve("hl7.fhir.r4.core-4.0.1.tgz"), tgz);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FhirIgPackageDownloader.DownloadOptions options = new FhirIgPackageDownloader.DownloadOptions(
                "https://packages.fhir.org", cacheDir.toString(), 60, false, false, true, true,
                new PrintStream(buffer));

        Path target = tempDir.resolve("extracted-from-cache");
        Path result = FhirIgPackageDownloader.downloadAndExtract(target, "hl7.fhir.r4.core", "4.0.1", options);

        assertEquals(target, result);
        assertTrue(Files.exists(target.resolve("StructureDefinition-Patient.json")));
        assertEquals(0, buffer.size(), "pre-seeded cache hit should need no network call and print no warning");
    }

    private static byte[] buildSamplePackageTgz() throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)) {
            addTarEntry(tarOut, "package/package.json", """
                    {"name":"test.package","version":"1.0.0","fhirVersions":["4.0.1"]}
                    """);
            addTarEntry(tarOut, "package/StructureDefinition-Patient.json", """
                    {
                      "resourceType": "StructureDefinition",
                      "url": "http://hl7.org/fhir/StructureDefinition/Patient",
                      "name": "Patient",
                      "type": "Patient",
                      "kind": "resource",
                      "fhirVersion": "4.0.1"
                    }
                    """);
            tarOut.finish();
        }
        return byteOut.toByteArray();
    }

    private static void addTarEntry(TarArchiveOutputStream tarOut, String name, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(bytes.length);
        tarOut.putArchiveEntry(entry);
        tarOut.write(bytes);
        tarOut.closeArchiveEntry();
    }
}
