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
