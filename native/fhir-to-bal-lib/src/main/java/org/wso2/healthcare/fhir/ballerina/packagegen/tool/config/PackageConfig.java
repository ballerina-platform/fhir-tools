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
    private String org;
    private String name;
    private String version;
    private String ballerinaDistribution;
    private String authors;
    private String repository;
    private String basePackage;
    private List<DependencyConfig> dependencyConfigList;

    public PackageConfig(JsonObject packageConfigJson) {
        this.org = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_ORG).getAsString();
        this.name = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_NAME).getAsString();
        this.version = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_VERSION).getAsString();
        this.ballerinaDistribution = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_DISTRIBUTION).getAsString();
        this.authors = packageConfigJson.getAsJsonArray(ToolConstants.CONFIG_PACKAGE_AUTHORS).getAsString();
        this.repository = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_REPOSITORY).getAsString();
        this.basePackage = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_BASE_PACKAGE).getAsString();
        populateDependencies(packageConfigJson.getAsJsonArray(ToolConstants.CONFIG_PACKAGE_DEPENDENCY).getAsJsonArray());
    }

    public PackageConfig(TomlTable packageConfigToml) {
        this.org = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_ORG_TOML);
        this.name = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_NAME_TOML);
        this.version = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_VERSION_TOML);
        this.ballerinaDistribution = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_DISTRIBUTION_TOML);
        this.authors = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_AUTHORS_TOML);
        this.repository = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_REPOSITORY_TOML);
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

    public String getAuthors() {
        return authors;
    }

    public String getRepository() {
        return repository;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public List<DependencyConfig> getDependencyConfigList() {
        return dependencyConfigList;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public void setName(String name) {
        this.name = name.replace("-","");
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setBallerinaDistribution(String ballerinaDistribution) {
        this.ballerinaDistribution = ballerinaDistribution;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public void setDependencyConfigList(List<DependencyConfig> dependencyConfigList) {
        this.dependencyConfigList = dependencyConfigList;
    }
}
