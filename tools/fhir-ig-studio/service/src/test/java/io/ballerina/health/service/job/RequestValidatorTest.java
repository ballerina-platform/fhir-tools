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

package io.ballerina.health.service.job;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the request rules the service shares with {@code bal health fhir}.
 */
class RequestValidatorTest {

    private static GenerationRequest templateFromRegistry() {
        GenerationRequest request = new GenerationRequest();
        request.setMode("template");
        GenerationRequest.SpecSource source = new GenerationRequest.SpecSource();
        source.setType("registry");
        source.setIgName("hl7.fhir.us.core");
        source.setIgVersion("6.1.0");
        request.setSource(source);
        return request;
    }

    private static GenerationRequest packageFromDefaultCore() {
        GenerationRequest request = new GenerationRequest();
        request.setMode("package");
        GenerationRequest.SpecSource source = new GenerationRequest.SpecSource();
        source.setType("default");
        source.setFhirVersion("r4");
        request.setSource(source);
        request.setPackageName("health.fhir.r4.uscore610");
        return request;
    }

    private static String errorOf(GenerationRequest request) {
        return assertThrows(JobManager.ValidationException.class, () -> RequestValidator.validate(request))
                .getMessage();
    }

    @Test
    void acceptsATemplateRequestFromTheRegistry() {
        assertDoesNotThrow(() -> RequestValidator.validate(templateFromRegistry()));
    }

    @Test
    void acceptsAPackageRequestFromTheDefaultCoreSpecification() {
        assertDoesNotThrow(() -> RequestValidator.validate(packageFromDefaultCore()));
    }

    @Test
    void rejectsAMissingRequest() {
        assertEquals("A generation request body is required.", errorOf(null));
    }

    @Test
    void rejectsAnUnsupportedMode() {
        GenerationRequest request = templateFromRegistry();
        request.setMode("connector");
        assertEquals("Mode must be one of [package, template].", errorOf(request));
    }

    @Test
    void rejectsAPackageRequestWithoutAPackageName() {
        GenerationRequest request = packageFromDefaultCore();
        request.setPackageName(null);
        assertEquals("Package name is required for package generation.", errorOf(request));
    }

    @Test
    void rejectsARegistrySourceWithoutAnIgName() {
        GenerationRequest request = templateFromRegistry();
        request.getSource().setIgName("  ");
        assertEquals("An implementation guide package name is required for the registry source.",
                errorOf(request));
    }

    @Test
    void rejectsAnUploadSourceWithoutASpecId() {
        GenerationRequest request = templateFromRegistry();
        request.getSource().setType("upload");
        assertEquals("An uploaded specification id is required for the upload source.", errorOf(request));
    }

    @Test
    void rejectsAnUnsupportedFhirVersionForTheDefaultSource() {
        GenerationRequest request = packageFromDefaultCore();
        request.getSource().setFhirVersion("r3");
        assertEquals("FHIR version must be r4 or r5 for the default core specification.", errorOf(request));
    }

    @Test
    void rejectsAMalformedDependentPackage() {
        GenerationRequest request = templateFromRegistry();
        request.setDependentPackage("not-a-package");
        assertEquals("Dependent package must be in the form 'org/package.name'.", errorOf(request));
    }

    @Test
    void acceptsAWellFormedDependentPackage() {
        GenerationRequest request = templateFromRegistry();
        request.setDependentPackage("ballerinax/health.fhir.r4.uscore501");
        assertDoesNotThrow(() -> RequestValidator.validate(request));
    }

    @Test
    void rejectsAnInvalidIgModuleName() {
        GenerationRequest request = templateFromRegistry();
        request.setIgModuleName("not a module");
        assertEquals("IG module name is not a valid Ballerina module name.", errorOf(request));
    }

    @Test
    void rejectsIncludedAndExcludedProfilesTogether() {
        GenerationRequest request = templateFromRegistry();
        request.setIncludedProfiles(List.of("http://example.org/a"));
        request.setExcludedProfiles(List.of("http://example.org/b"));
        assertEquals("Included and excluded profiles cannot be used together.", errorOf(request));
    }

    @Test
    void rejectsBlankProfileEntries() {
        GenerationRequest request = templateFromRegistry();
        request.setIncludedProfiles(List.of("http://example.org/a", " "));
        assertEquals("The included profiles list contains an empty entry.", errorOf(request));
    }

    @Test
    void rejectsResourceSelectionForPackageMode() {
        GenerationRequest request = packageFromDefaultCore();
        request.setResources(List.of("Patient"));
        assertEquals("Resource selection applies to template generation only.", errorOf(request));
    }

    @Test
    void rejectsANonSemanticPackageVersion() {
        GenerationRequest request = packageFromDefaultCore();
        request.setPackageVersion("1.0");
        assertEquals("Package version must follow semantic versioning, for example 1.0.0.", errorOf(request));
    }

    @Test
    void rejectsAPackageNameWithIllegalCharacters() {
        GenerationRequest request = packageFromDefaultCore();
        request.setPackageName("my-package");
        assertEquals("Package name may only contain letters, digits, '.' and '_'.", errorOf(request));
    }

    @Test
    void rejectsAnOrganizationNameWithIllegalCharacters() {
        GenerationRequest request = packageFromDefaultCore();
        request.setOrgName("my org");
        assertEquals("Organization name may only contain letters, digits and '_'.", errorOf(request));
    }
}
