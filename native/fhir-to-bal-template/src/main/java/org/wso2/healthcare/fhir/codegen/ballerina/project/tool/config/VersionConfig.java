/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config;

import com.google.gson.JsonObject;

public class VersionConfig {
    private String namePrefix;
    private String basePackage;
    private String servicePackage;
    private String dependentPackage;

    public VersionConfig(JsonObject versionConfigObj) {
        this.namePrefix = versionConfigObj.getAsJsonPrimitive("namePrefix").getAsString();
        this.basePackage = versionConfigObj.getAsJsonPrimitive("basePackage").getAsString();
        this.servicePackage = versionConfigObj.getAsJsonPrimitive("servicePackage").getAsString();
        this.dependentPackage = versionConfigObj.getAsJsonPrimitive("dependentPackage").getAsString();
    }

    public String getNamePrefix(){
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getServicePackage() {
        return servicePackage;
    }

    public void setServicePackage(String servicePackage) {
        this.servicePackage = servicePackage;
    }

    public String getDependentPackage() {
        return dependentPackage;
    }

    public void setDependentPackage(String dependentPackage) {
        this.dependentPackage = dependentPackage;
    }
}
