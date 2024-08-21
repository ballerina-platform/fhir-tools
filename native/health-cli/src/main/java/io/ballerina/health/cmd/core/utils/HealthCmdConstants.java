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
    public static final String CMD_SUB_FHIR = "fhir";
    public static final String CMD_SUB_HL7 = "hl7";
    public static final String CMD_SUB_CDS = "cds";
    public static final String CMD_CONFIG_FILENAME = "tool-config.json";
    public static final String CMD_CDS_CONFIG_FILENAME = "cds-tool-config.json";
    public static final String CMD_CDS_JSON_SCHEMA_FILENAME = "cds-hooks-json-schema.json";
    public static final String CMD_MVN_ARTIFACT_NAME = "health-tools";
    public static final String CMD_RESOURCE_PATH_SUFFIX = "/modules/health/resources";
    public static final String CMD_HELP_TEXT_FILENAME = "ballerina-health.help";
    public static final String CMD_CDS_HELP_TEXT_FILENAME = "ballerina-health-cds.help";
    public static final String CMD_DEFAULT_IG_NAME = "FHIR";
    public static final String CMD_DEFAULT_ORG_NAME = "healthcare";
    public static final String CMD_FHIR_MODE_TEMPLATE = "fhir:template";
    public static final String CMD_FHIR_MODE_CLIENT = "fhir:client";
    public static final String CMD_FHIR_MODE_PACKAGE = "fhir:package";
    public static final String CMD_CDS_MODE_TEMPLATE = "cds:template";
    public static final String CDS = "cds";
    public static final String CDS_SUB_TOOL_DESCRIPTION = "Generates Ballerina service for provided cds hook definitions.";
    public static final String TOOLS = "tools";
    public static final String HOOKS = "hooks";
    public static final String CDS_CONFIG_CLASS_NAME = "org.wso2.healthcare.cds.codegen.ballerina.tool.config.BallerinaProjectToolConfig";
    public static final String CDS_TOOL_CLASS_NAME = "org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectTool";
    public static final String PROJECT_PACKAGE_ORG = "project.package.org";
    public static final String PROJECT_PACKAGE_VERSION = "project.package.version";
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
    public static final String CMD_OPTION_PACKAGE_VERSION_DESCRIPTION = "version of the Ballerina package";
    public static final String CMD_OPTION_DEPENDENT_PACKAGE = "--dependent-package";
    public static final String CMD_OPTION_CUSTOM_ARGS_DESCRIPTION = "Custom arguments";


    public class PrintStrings {

        public static final String HELP_FOR_MORE_INFO = "[INFO] Try bal health --help for more information.";
        public static final String TEMPLATE_GEN_SUCCESS = "[INFO] Ballerina templates generation completed " +
                "successfully. Generated templates can be found at: ";
        public static final String PKG_GEN_SUCCESS = "[INFO] Ballerina FHIR package generation completed successfully."
                + " Generated package can be found at: ";

        public static final String INVALID_SPEC_PATH = "[ERROR] Invalid specification path received.";
        public static final String HELP_NOT_AVAILABLE = "[ERROR] Helper text is not available.";
        public static final String HELP_ERROR = "[ERROR] An Error occurred internally while fetching the Help text.";
        public static final String INVALID_NUM_OF_ARGS = "[ERROR] Invalid number of arguments received for the command.";
        public static final String INVALID_MODE = "[ERROR] Invalid mode received for tool command.";
        public static final String PKG_NAME_REQUIRED = "[ERROR] Package name [--package-name] is required for package "
                + "generation.";
        public static final String DEPENDENT_REQUIRED = "[ERROR] Dependent package [--dependent-package] is required "
                + "for template generation.";
        public static final String DEPENDENT_INCORRECT = "[ERROR] Format of the dependent package is incorrect.";
        public static final String INCLUDED_EXCLUDED_TOGETHER = "[ERROR] Both --included-profile and "
                + "--excluded-profile cannot be used together.";
    }

}
