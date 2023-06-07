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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.consensys.cava.toml.TomlArray;
import net.consensys.cava.toml.TomlTable;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Ballerina package level config.
 */
public class PackageConfig {
    private final String org;
    private final String name;
    private final String version;
    private final String ballerinaDistribution;
    private final String basePackage;
    private List<DependencyConfig> dependencyConfigList;

    public PackageConfig(JsonObject packageConfigJson) {
        this.org = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_ORG).getAsString();
        this.name = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_NAME).getAsString();
        this.version = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_VERSION).getAsString();
        this.ballerinaDistribution = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_DISTRIBUTION).getAsString();
        this.basePackage = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_BASE_PACKAGE).getAsString();
        populateDependencies(packageConfigJson.getAsJsonArray(ToolConstants.CONFIG_PACKAGE_DEPENDENCY).getAsJsonArray());
    }

    public PackageConfig(TomlTable packageConfigToml) {
        this.org = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_ORG_TOML);
        this.name = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_NAME_TOML);
        this.version = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_VERSION_TOML);
        this.ballerinaDistribution = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_DISTRIBUTION_TOML);
        this.basePackage = packageConfigToml.getString(ToolConstants.CONFIG_BASE_PACKAGE_TOML);
        populateDependencies(packageConfigToml.getArrayOrEmpty(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_TOML));
    }

    private void populateDependencies(JsonArray dependencyArray) {
        dependencyConfigList = new ArrayList<>();
        for (int i = 0; i < dependencyArray.size(); i++) {
            dependencyConfigList.add(new DependencyConfig(dependencyArray.get(i).getAsJsonObject()));
        }
    }

    private void populateDependencies(TomlArray dependencyArray) {
        dependencyConfigList = new ArrayList<>();
        for (int i = 0; i < dependencyArray.size(); i++) {
            TomlTable item = dependencyArray.getTable(i);
            if (item != null) {
                dependencyConfigList.add(new DependencyConfig(item));
            }
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

    public String getBallerinaDistribution() {
        return ballerinaDistribution;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public List<DependencyConfig> getDependencyConfigList() {
        return dependencyConfigList;
    }
}
