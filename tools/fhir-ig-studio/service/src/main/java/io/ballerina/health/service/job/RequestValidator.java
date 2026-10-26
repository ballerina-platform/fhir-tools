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

import io.ballerina.health.cmd.core.utils.IgModuleNameUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Applies the same request rules the {@code bal health fhir} command enforces, so a job accepted by the service
 * behaves like the equivalent CLI invocation.
 */
public final class RequestValidator {

    // Ordered lists rather than sets: these are interpolated into error messages, and Set.of iteration order
    // varies from one JVM run to the next.
    private static final List<String> SUPPORTED_MODES = List.of("package", "template");
    private static final List<String> SUPPORTED_SOURCE_TYPES = List.of("registry", "upload", "default");
    private static final List<String> SUPPORTED_FHIR_VERSIONS = List.of("r4", "r5");

    /** Matches {@code org/package.name} as accepted by the CLI. */
    private static final Pattern DEPENDENT_PACKAGE_PATTERN = Pattern.compile(
            "^(?!.*__)[a-zA-Z0-9][a-zA-Z0-9_]+[a-zA-Z0-9]/[a-zA-Z0-9][a-zA-Z0-9._]+[a-zA-Z0-9]$");

    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._]*$");
    private static final Pattern ORG_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_]*$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+([.\\-+][a-zA-Z0-9.\\-+]+)?$");

    private RequestValidator() {
    }

    public static void validate(GenerationRequest request) throws JobManager.ValidationException {
        if (request == null) {
            throw invalid("A generation request body is required.");
        }
        String mode = request.getMode();
        if (mode == null || !SUPPORTED_MODES.contains(mode)) {
            throw invalid("Mode must be one of " + SUPPORTED_MODES + ".");
        }

        GenerationRequest.SpecSource source = request.getSource();
        if (!SUPPORTED_SOURCE_TYPES.contains(source.getType())) {
            throw invalid("Specification source type must be one of " + SUPPORTED_SOURCE_TYPES + ".");
        }
        if ("registry".equals(source.getType()) && isBlank(source.getIgName())) {
            throw invalid("An implementation guide package name is required for the registry source.");
        }
        if ("upload".equals(source.getType()) && isBlank(source.getSpecId())) {
            throw invalid("An uploaded specification id is required for the upload source.");
        }
        if ("default".equals(source.getType()) && !SUPPORTED_FHIR_VERSIONS.contains(source.getFhirVersion())) {
            throw invalid("FHIR version must be r4 or r5 for the default core specification.");
        }

        if ("package".equals(mode) && isBlank(request.getPackageName())) {
            throw invalid("Package name is required for package generation.");
        }
        if (!isBlank(request.getPackageName())
                && !PACKAGE_NAME_PATTERN.matcher(request.getPackageName().trim()).matches()) {
            throw invalid("Package name may only contain letters, digits, '.' and '_'.");
        }
        if (!isBlank(request.getOrgName())
                && !ORG_NAME_PATTERN.matcher(request.getOrgName().trim()).matches()) {
            throw invalid("Organization name may only contain letters, digits and '_'.");
        }
        if (!isBlank(request.getPackageVersion())
                && !VERSION_PATTERN.matcher(request.getPackageVersion().trim()).matches()) {
            throw invalid("Package version must follow semantic versioning, for example 1.0.0.");
        }
        if (!isBlank(request.getDependentPackage())
                && !DEPENDENT_PACKAGE_PATTERN.matcher(request.getDependentPackage().trim()).matches()) {
            throw invalid("Dependent package must be in the form 'org/package.name'.");
        }
        if (!isBlank(request.getIgModuleName())
                && !IgModuleNameUtils.isValidBallerinaModuleName(request.getIgModuleName().trim())) {
            throw invalid("IG module name is not a valid Ballerina module name.");
        }
        if (!request.getIncludedProfiles().isEmpty() && !request.getExcludedProfiles().isEmpty()) {
            throw invalid("Included and excluded profiles cannot be used together.");
        }
        if ("package".equals(mode) && !request.getResources().isEmpty()) {
            throw invalid("Resource selection applies to template generation only.");
        }
        rejectBlankEntries(request.getIncludedProfiles(), "included profiles");
        rejectBlankEntries(request.getExcludedProfiles(), "excluded profiles");
        rejectBlankEntries(request.getResources(), "resources");
    }

    private static void rejectBlankEntries(List<String> values, String field) throws JobManager.ValidationException {
        for (String value : values) {
            if (isBlank(value)) {
                throw invalid("The " + field + " list contains an empty entry.");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static JobManager.ValidationException invalid(String message) {
        return new JobManager.ValidationException(message);
    }
}
