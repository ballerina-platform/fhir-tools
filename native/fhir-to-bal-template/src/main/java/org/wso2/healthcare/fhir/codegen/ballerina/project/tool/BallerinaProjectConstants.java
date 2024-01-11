/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool;

/**
 * Constants for Ballerina Project Gen tool
 */
public class BallerinaProjectConstants {
    public static final String CONFIG_ENABLE = "enable";
    public static final String CONFIG_PROFILE_IG = "implementationGuide";
    public static final String RESOURCE_PATH_TEMPLATES = "template";
    public static final String PROJECT_API_SUFFIX = ".api";
    public static final String BASE_PACKAGE_IMPORT_SUFFIX = "ballerinax/health.fhir.";
    public static final String SERVICE_PACKAGE_IMPORT_SUFFIX = "ballerinax/health.fhir";
    public static final String INTERNATIONAL_PACKAGE_IMPORT_SUFFIX = "ballerinax/health.fhir.r4.international401";

    public class PrintStrings {

        public static final String OVERWRITING_EXISTING_TEMPLATES = "[INFO] Overwriting the existing templates.";
        public static final String TEMPLATES_FOR_ALL_PROFILES = "[INFO] Generating templates for all FHIR profiles...";

        public static final String INVALID_PROFILE = "[WARN] Invalid FHIR profile: ";

        public static final String INVALID_INPUT = "[ERROR] Invalid input. Exiting the tool.";
    }

    public static class CapabilityStmt {
        public static final String REFERENCE_SERVER_CAPABILITIES = "referenceServerCapabilities";
        public static final String CAPABILITY_STATEMENT = "CapabilityStatement";
        public static final String STRUCTURE_DEFINITION = "StructureDefinition";
    }

    public static class BalCentral {
        public static final String CENTRAL_URL = "https://api.central.ballerina.io/2.0/";
        public static final String PACKAGES_PATH = "/registry/packages/";
        public static final String DOCS_PATH = "/docs/";
        public static final String IMPORT_STATEMENT = "importStatement";
        public static final String DOCS_DATA = "docsData";
        public static final String MODULES = "modules";
        public static final String CONSTANTS = "constants";
        public static final String RECORDS = "records";
        public static final String VALUE = "value";
        public static final String NAME = "name";
        public static final String PROFILE_BASE_PREFIX = "PROFILE_BASE_";
    }
}
