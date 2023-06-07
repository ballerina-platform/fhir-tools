// Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.config;

import com.google.gson.JsonObject;
import net.consensys.cava.toml.TomlTable;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;

/**
 * Ballerina Dependencies related config.
 */
public class DependencyConfig {
    private final String org;
    private final String name;
    private final String version;
    private String repository;

    public DependencyConfig(String org, String name, String version, String repository) {
        this.org = org;
        this.name = name;
        this.version = version;
        this.repository = repository;
    }

    public DependencyConfig(String org, String name, String version) {
        this.org = org;
        this.name = name;
        this.version = version;
    }

    public DependencyConfig(JsonObject implementationGuide) {
        this.org = implementationGuide.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_ORG).getAsString();
        this.name = implementationGuide.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_NAME).getAsString();
        this.version = implementationGuide.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_VERSION).getAsString();
        if (implementationGuide.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_REPOSITORY) != null) {
            this.repository = implementationGuide.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_REPOSITORY).getAsString();
        }
    }

    public DependencyConfig(TomlTable implementationGuide) {
        this.org = implementationGuide.getString(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_ORG_TOML);
        this.name = implementationGuide.getString(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_NAME_TOML);
        this.version = implementationGuide.getString(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_VERSION_TOML);

        if (implementationGuide.getString(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_REPOSITORY) != null) {
            this.repository = implementationGuide.getString(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_REPOSITORY_TOML);
        }
    }

    public String getOrg() {
        return org;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getRepository() {
        return repository;
    }
}
