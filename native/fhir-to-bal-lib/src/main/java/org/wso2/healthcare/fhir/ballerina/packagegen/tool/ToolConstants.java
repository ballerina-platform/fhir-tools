// Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

package org.wso2.healthcare.fhir.ballerina.packagegen.tool;

public class ToolConstants {
    public static final String CONFIG_ENABLE = "enable";
    public static final String CONFIG_PROFILE_IG = "implementationGuide";
    public static final String CONFIG_PROFILE_IG_TOML = "implementation_guide";
    public static final String CONFIG_PROFILE_IG_BASE = "baseIGPackage";
    public static final String CONFIG_PROFILE_IG_BASE_TOML = "base_ig_package";
    public static final String CONFIG_PACKAGE = "packageConfigs";
    public static final String CONFIG_PACKAGE_TOML = "tools.config.package";
    public static final String CONFIG_PACKAGE_ORG = "org";
    public static final String CONFIG_PACKAGE_ORG_TOML = "tools.config.package.org";
    public static final String CONFIG_PACKAGE_NAME = "name";
    public static final String CONFIG_PACKAGE_NAME_TOML = "tools.config.package.name";
    public static final String CONFIG_PACKAGE_VERSION = "version";
    public static final String CONFIG_PACKAGE_VERSION_TOML = "tools.config.package.version";
    public static final String CONFIG_PACKAGE_DISTRIBUTION = "ballerinaDistribution";
    public static final String CONFIG_PACKAGE_DISTRIBUTION_TOML = "tools.config.package.ballerina_distribution";
    public static final String CONFIG_BASE_PACKAGE = "basePackage";
    public static final String CONFIG_BASE_PACKAGE_TOML = "tools.config.package.utils_package";
    public static final String CONFIG_PACKAGE_DEPENDENCY = "dependencies";
    public static final String CONFIG_PACKAGE_DEPENDENCY_TOML = "tools.config.package.dependency";
    public static final String CONFIG_PACKAGE_DEPENDENCY_ORG = "org";
    public static final String CONFIG_PACKAGE_DEPENDENCY_ORG_TOML = "tools.config.package.dependency.org";
    public static final String CONFIG_PACKAGE_DEPENDENCY_NAME = "name";
    public static final String CONFIG_PACKAGE_DEPENDENCY_NAME_TOML = "tools.config.package.dependency.name";
    public static final String CONFIG_PACKAGE_DEPENDENCY_VERSION = "version";
    public static final String CONFIG_PACKAGE_DEPENDENCY_VERSION_TOML = "tools.config.package.dependency.version";
    public static final String CONFIG_PACKAGE_DEPENDENCY_REPOSITORY = "repository";
    public static final String CONFIG_PACKAGE_DEPENDENCY_REPOSITORY_TOML = "tools.config.package.dependency.repository";
    public static final String CONFIG_INCLUDED_IGS = "includedIGs";
    public static final String CONFIG_INCLUDED_IGS_TOML = "tools.config.included_igs";
    public static final String CONFIG_DATA_TYPE_MAPPINGS = "dataTypeMappings";
    public static final String CONFIG_DATA_TYPE_MAPPING_TOML = "tools.config.data_type_mappings";
    public static final String CONFIG_DATA_TYPE_FHIR = "fhirType";
    public static final String CONFIG_DATA_TYPE_FHIR_TOML = "tools.config.data_type_mappings.fhir_type";
    public static final String CONFIG_DATA_TYPE_BALLERINA = "ballerinaType";
    public static final String CONFIG_DATA_TYPE_BALLERINA_TOML = "tools.config.data_type_mappings.ballerina_type";
    public static final String CONFIG_BALLERINA_KEYWORD = "ballerinaKeywords";
    public static final String CONFIG_BALLERINA_KEYWORD_TOML = "tools.config.ballerina_keyword";
    public static final String CONFIG_BALLERINA_KEYWORD_KEYWORD = "keyword";
    public static final String CONFIG_BALLERINA_KEYWORD_KEYWORD_TOML = "tools.config.ballerina_keyword.keyword";
    public static final String CONFIG_BALLERINA_KEYWORD_REPLACE = "replace";
    public static final String CONFIG_BALLERINA_KEYWORD_REPLACE_TOML = "tools.config.ballerina_keyword.replace";

    public static final String BASE_IG = "international";
    public static final String TEMPLATE_PATH = "templates";
    public static final String R4_BASE_PATH = "r4base/packages/healthcare.fhir.r4";
    public static final String GENERATION_DIR = "generated-project";
    public static final String BAL_EXTENSION = ".bal";
    public static final String TOML_EXTENSION = ".toml";
    public static final String MD_EXTENSION = ".md";
    public static final String CONSTRAINTS_LIB_IMPORT = "ballerina/constraint";

    public enum TokenPosition {
        BEGIN,
        MIDDLE,
        END
    }

    public static final String LICENSE_YEAR = "2023";
}