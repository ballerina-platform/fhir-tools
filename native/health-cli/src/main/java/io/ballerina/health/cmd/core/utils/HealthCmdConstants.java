/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.health.cmd.core.utils;

public class HealthCmdConstants {

    public static final String CMD_MODE_TEMPLATE = "template";
    public static final String CMD_MODE_PACKAGE = "package";
    public static final String CMD_MODE_CONNECTOR = "connector";
    public static final String CMD_SUB_FHIR = "fhir";
    public static final String CMD_SUB_HL7 = "hl7";
    public static final String CMD_SUB_CDS = "cds";
    public static final String CMD_CONFIG_FILENAME = "tool-config.json";
    public static final String CMD_CDS_CONFIG_FILENAME = "cds-tool-config.json";
    public static final String CMD_CDS_JSON_SCHEMA_FILENAME = "cds-hooks-json-schema.json";
    public static final String CMD_CONNECTOR_CONFIG_FILENAME = "connector-tool-config.json";
    public static final String CMD_MVN_ARTIFACT_NAME = "health-tools";
    public static final String CMD_RESOURCE_PATH_SUFFIX = "/modules/health/resources";
    public static final String CMD_HELP_TEXT_FILENAME = "ballerina-health.help";
    public static final String CMD_CDS_HELP_TEXT_FILENAME = "ballerina-health-cds.help";
    public static final String CMD_DEFAULT_IG_NAME = "FHIR";
    public static final String CMD_DEFAULT_ORG_NAME = "healthcare";
    public static final String CMD_DEFAULT_R4_DEPENDENT_PACKAGE = "ballerinax/health.fhir.r4.international401";
    public static final String CMD_DEFAULT_R5_DEPENDENT_PACKAGE = "ballerinax/health.fhir.r5.international500";
    public static final String CMD_DEFAULT_INTERNATIONAL_IG_DIR = "international";
    public static final String CMD_DEFAULT_REGISTRY_URL = "https://packages.fhir.org";
    public static final String CMD_DEFAULT_IG_CACHE_DIR = ".fhir-ig-cache";
    public static final int CMD_DEFAULT_IG_HTTP_TIMEOUT_SECONDS = 60;
    public static final String CMD_DEFAULT_R4_IG_PACKAGE_NAME = "hl7.fhir.r4.core";
    public static final String CMD_DEFAULT_R4_IG_PACKAGE_VERSION = "4.0.1";
    public static final String CMD_DEFAULT_R5_IG_PACKAGE_NAME = "hl7.fhir.r5.core";
    public static final String CMD_DEFAULT_R5_IG_PACKAGE_VERSION = "5.0.0";
    public static final String CMD_OPTION_IG = "--ig";
    public static final String CMD_OPTION_REGISTRY_URL = "--registry-url";
    public static final String CMD_OPTION_IG_CACHE_DIR = "--ig-cache-dir";
    public static final String CMD_OPTION_FORCE_IG_DOWNLOAD = "--force-ig-download";
    public static final String CMD_FHIR_MODE_TEMPLATE = "fhir:template";
    public static final String CMD_FHIR_MODE_CLIENT = "fhir:client";
    public static final String CMD_FHIR_MODE_PACKAGE = "fhir:package";
    public static final String CMD_FHIR_MODE_CONNECTOR = "fhir:connector";
    public static final String CMD_CDS_MODE_TEMPLATE = "cds:template";
    public static final String CDS = "cds";
    public static final String CDS_SUB_TOOL_DESCRIPTION = "Generates Ballerina service for provided cds hook definitions.";
    public static final String TOOLS = "tools";
    public static final String HOOKS = "hooks";
    public static final String CDS_CONFIG_CLASS_NAME = "org.wso2.healthcare.cds.codegen.ballerina.tool.config.BallerinaCDSProjectToolConfig";
    public static final String CDS_TOOL_CLASS_NAME = "org.wso2.healthcare.cds.codegen.ballerina.tool.BallerinaCDSProjectTool";
    public static final String PROJECT_PACKAGE_ORG = "project.package.org";
    public static final String PROJECT_PACKAGE_VERSION = "project.package.version";
    public static final String PROJECT_PACKAGE_BASE_PACKAGE = "project.package.basePackage";
    public static final String PROJECT_PACKAGE_DEPENDENT_PACKAGE = "project.package.dependentPackage";
    public static final String PROJECT_PACKAGE_NAME_PREFIX = "project.package.namePrefix";
    public static final String USER_DIR = "user.dir";
    public static final String SEMICOLON = ":";

    public static final String CMD_OPTION_HELP = "--help";
    public static final String CMD_OPTION_HELP_SHORTER_1 = "-h";
    public static final String CMD_OPTION_HELP_SHORTER_2 = "?";
    public static final String CMD_OPTION_MODE = "--mode";
    public static final String CMD_OPTION_MODE_SHORTER = "-m";
    public static final String CMD_OPTION_CDS_MODE_DESCRIPTION = "Execution mode. Only \"template\" option is supported.";
    public static final String CMD_OPTION_OUTPUT = "--output";
    public static final String CMD_OPTION_OUTPUT_SHORTER = "-o";
    public static final String CMD_OPTION_OUTPUT_DESCRIPTION = "Location of the generated Ballerina artifacts.";
    public static final String CMD_OPTION_PACKAGE_NAME = "--package-name";
    public static final String CMD_OPTION_PACKAGE_NAME_DESCRIPTION = "Name of the Ballerina package";
    public static final String CMD_OPTION_ORG_NAME = "--org-name";
    public static final String CMD_OPTION_ORG_NAME_DESCRIPTION = "Organization name of the Ballerina package";
    public static final String CMD_OPTION_PACKAGE_VERSION = "--package-version";
    public static final String CMD_OPTION_PACKAGE_VERSION_DESCRIPTION = "Location of the CDS hooks definition file";
    public static final String CMD_OPTION_DEPENDENT_PACKAGE = "--dependent-package";
    public static final String CMD_OPTION_INPUT = "--input";
    public static final String CMD_OPTION_INPUT_SHORTER = "-i";
    public static final String CMD_OPTION_CUSTOM_ARGS_DESCRIPTION = "Custom arguments";
    public static final String CMD_CONNECTOR = "connector";
    public static final String CMD_OPTION_MINIMAL = "--minimal";
    public static final String CMD_OPTION_MINIMAL_DESCRIPTION = "Generate templates without package structure and metadata files";


    public class PrintStrings {

        public static final String HELP_FOR_MORE_INFO = "[INFO] Try bal health --help for more information.";
        public static final String TEMPLATE_GEN_SUCCESS_MESSAGE = "[INFO] Ballerina templates generation completed " +
                "successfully. Generated templates can be found at: ";
        public static final String CDS_TEMPLATE_GEN_SUCCESS_MESSAGE = "[INFO] Ballerina CDS service template generation completed " +
                "successfully. The generated project can be found at ";
        public static final String PKG_GEN_SUCCESS = "[INFO] Ballerin" +
                "a FHIR package generation completed successfully."
                + " Generated package can be found at: ";

        public static final String INVALID_SPEC_PATH = "[ERROR] Invalid specification path received.";
        public static final String CONNECTOR_GEN_SUCCESS = "[INFO] Ballerina FHIR connector generation completed successfully."
                + " Generated connector can be found at: ";
        public static final String HELP_NOT_AVAILABLE = "[ERROR] Helper text is not available.";
        public static final String HELP_ERROR = "[ERROR] An Error occurred internally while fetching the Help text.";
        public static final String INVALID_NUM_OF_ARGS = "[ERROR] Invalid number of arguments received for the command.";
        public static final String NO_INPUT_FILE_PATH = "[ERROR] Cannot find the input[-i] path argument.";
        public static final String EMPTY_INPUT_FILE_PATH = "[ERROR] The input[-i] path argument is empty.";
        public static final String INVALID_INPUT_FILE_PATH = "[ERROR] Cannot find the file provided for the input[-i] path argument.";
        public static final String INVALID_MODE = "[ERROR] Invalid mode received for tool command.";
        public static final String PKG_NAME_REQUIRED = "[ERROR] Package name [--package-name] is required for package "
                + "generation.";
        public static final String GEN_ERROR = "[ERROR] Error occurred while generating the Ballerina artifacts.";
        public static final String DEPENDENT_REQUIRED = "[ERROR] --dependent-package, --ig, or a resolvable spec "
                + "path (local or registry download) is required for template generation.";
        public static final String IG_DOWNLOAD_SUCCESS = "[INFO] Downloaded IG package ";
        public static final String SPEC_PATH_REQUIRED = "[ERROR] FHIR specification path or --ig is required.";
        public static final String USING_INTERNATIONAL_BASE = "[INFO] No custom IG specified. Using international "
                + "base FHIR structure definitions with ";
        public static final String DEPENDENT_INCORRECT = "[ERROR] Format of the dependent package is incorrect.";
        public static final String IG_REFERENCE_INVALID = "[ERROR] Invalid --ig value. Expected <name>[@version], "
                + "e.g. hl7.fhir.us.core@8.0.1.";
        public static final String INCLUDED_EXCLUDED_TOGETHER = "[ERROR] Both --included-profile and "
                + "--excluded-profile cannot be used together.";
        public static final String CDS_HOOKS_VALIDATION = "[ERROR] CDS hooks validation failed!";
        public static final String INVALID_CONFIG_PATH = "[ERROR] Cannot find the configuration file provided for the input[-i] path argument.";
        public static final String IG_CACHE_MISS_WARNING = "[WARN] Expected cached IG package not found: ";
        public static final String IG_CACHE_GITIGNORE_WARNING = "[WARN] Add the IG cache directory to your "
                + ".gitignore if it isn't already ignored: ";
        public static final String IG_VERSION_MISMATCH_WARNING = "[WARN] Replacing existing IG definitions at ";
        public static final String MULTI_IG_TEMPLATE_ONLY = "[ERROR] Multiple --ig values are only supported in "
                + "template mode (-m template).";
        public static final String MULTI_IG_DEPENDENT_PACKAGE_CONFLICT = "[ERROR] --dependent-package cannot be "
                + "combined with multiple --ig values; each IG's dependent package is resolved automatically via "
                + "packageMappings.";

    }

}
