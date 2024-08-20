/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.cds.codegen.ballerina.tool;

/**
 * Constants for Ballerina Project Gen tool
 */
public class CdsBallerinaProjectConstants {
    public static final String CONFIG_ENABLE = "enable";
    public static final String RESOURCE_PATH_TEMPLATES = "template";
    public static final String TEMPLATE_NAME = "templateName";
    public static final String KEYWORDS = "keywords";
    public static final String RESOURCE_PATH_SEPARATOR = "/";
    public static final String CONFIG = "config";
    public static final String PACKAGE = "package";
    public static final String DEPENDENCIES = "dependencies";
    public static final String DEPENDENT_PACKAGE = "dependentPackage";
    public static final String HOOKS = "hooks";
    public static final String CDS_SERVICES = "cds_services";
    public static final String META_CONFIG = "metaConfig";
    public static final String PROJECT_PACKAGE_ORG = "project.package.org";
    public static final String PROJECT_PACKAGE_VERSION = "project.package.version";
    public static final String PROJECT_PACKAGE_DEPENDENT_PACKAGE = "project.package.dependentPackage";
    public static final String PROJECT_PACKAGE_NAME_PREFIX = "project.package.namePrefix";
    public static final String CMD_MESSAGE_OVERRIDE_OUTPUT_DIRECTORY = "Generated templates already exists. Do you want to overwrite? (y/n): ";
    public static final String YES = "y";
    public static final String NO = "n";
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final String SERVICE = "service";
    public static final String REGEX_STRING_FOR_UNDERSCORE_AND_HYPHEN = "[-|_]";
    public static final String REGEX_STRING_FOR_NON_WORD_CHARACTER = "\\W+";
    public static final String UNDERSCORE = "_";
    public static final String BAL_SERVICE_VM = "cdsBalService.vm";
    public static final String BAL_INTERCEPTOR_VM = "cdsInterceptor.vm";
    public static final String BAL_UTILS_VM = "cdsUtils.vm";
    public static final String BAL_DECISION_SYSTEM_CONNECTION_VM = "implDecisionSystemConnection.vm";
    public static final String BAL_FEEDBACK_SYSTEM_CONNECTION_VM = "implFeedbackSystemConnection.vm";
    public static final String BAL_PACKAGE_MD_VM = "cdsPackageMd.vm";
    public static final String BAL_GIT_IGNORE_VM = "cdsGitignore.vm";
    public static final String BAL_TOML_VM = "cdsBallerinaToml.vm";
    public static final String BAL_CONFIG_TOML_VM = "cdsConfigToml.vm";
    public static final String BAL_SERVICE_FILE = "service.bal";
    public static final String BAL_INTERCEPTOR_FILE = "interceptor.bal";
    public static final String BAL_UTILS_FILE = "utils.bal";
    public static final String BAL_DECISION_SYSTEM_CONNECTION_FILE = "implDecisionSystemConnection.bal";
    public static final String BAL_FEEDBACK_SYSTEM_CONNECTION_FILE = "implFeedbackSystemConnection.bal";
    public static final String BAL_PACKAGE_MD_FILE = "Package.md";
    public static final String BAL_GIT_IGNORE_FILE = ".gitignore";
    public static final String BAL_TOML_FILE = "Ballerina.toml";
    public static final String BAL_CONFIG_TOML_FILE = "Config.toml";
    public static final String ID = "id";
    public static final String HOOK = "hook";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String PREFETCH = "prefetch";
    public static final String USAGE_REQUIREMENTS = "usageRequirements";
    public static final String ORG = "org";
    public static final String NAME_PREFIX = "namePrefix";
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String DISTRIBUTION = "distribution";
    public static final String AUTHORS = "authors";

    public static class PrintStrings {
        public static final String OVERWRITING_EXISTING_TEMPLATES = "[INFO] Overwriting the existing templates.";
        public static final String INVALID_INPUT = "[ERROR] Invalid input. Exiting the tool.";
    }
}
