/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.com).
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

/**
 * This class represents the interaction configuration.
 */
public class InteractionConfig {

    private String name;
    private boolean isEnabled;

    public InteractionConfig(JsonObject interaction) {
        this.name = interaction.get("name").getAsString();
        this.isEnabled = interaction.get("enable").getAsBoolean();
    }

    /**
     * Get the name of the interaction.
     * @return name of the interaction.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the status of the interaction.
     * @return status of the interaction.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    public String getNameUppercase(){
        return name.toUpperCase();
    }

}
