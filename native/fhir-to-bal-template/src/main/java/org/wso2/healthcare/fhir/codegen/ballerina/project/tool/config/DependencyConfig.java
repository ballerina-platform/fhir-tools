/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config;

import com.google.gson.JsonObject;

public class DependencyConfig {
    private final String org;
    private final String name;
    private final String version;
    private final String importStatement;
    public DependencyConfig(JsonObject packageConfig) {
        this.org = packageConfig.getAsJsonPrimitive("org").getAsString();
        this.name = packageConfig.getAsJsonPrimitive("name").getAsString();
        this.version = packageConfig.getAsJsonPrimitive("version").getAsString();
        this.importStatement = packageConfig.getAsJsonPrimitive("importStatement").getAsString();
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

    public String getImportStatement() {
        return importStatement;
    }
}