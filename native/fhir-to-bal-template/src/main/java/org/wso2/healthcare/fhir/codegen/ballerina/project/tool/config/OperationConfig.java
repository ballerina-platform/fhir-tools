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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class OperationConfig {

    private String name;
    private boolean isActive;
    private List<String> includedResources = new ArrayList<>();

    public OperationConfig(JsonObject operationConfig){
        this.isActive = operationConfig.getAsJsonPrimitive("isActive").getAsBoolean();
        this.name = operationConfig.getAsJsonPrimitive("name").getAsString();
        for (JsonElement resource:operationConfig.getAsJsonArray("includeIn")
             ) {
            includedResources.add(resource.getAsString());
        }
    }

    public OperationConfig(String name){
        this.name = name;
        this.isActive = true;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return isActive;
    }

    public List<String> getIncludedResources() {
        return includedResources;
    }
}
