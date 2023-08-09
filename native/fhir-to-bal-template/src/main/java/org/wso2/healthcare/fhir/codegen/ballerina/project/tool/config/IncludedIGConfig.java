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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.consensys.cava.toml.TomlArray;
import net.consensys.cava.toml.TomlTable;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;

import java.util.ArrayList;
import java.util.List;

public class IncludedIGConfig {
    private final String name;
    private String importStatement;
    private final boolean isEnable;
    private final List<String> includedProfiles = new ArrayList<>();
    private final List<String> excludedProfiles = new ArrayList<>();

    public IncludedIGConfig(JsonObject implementationGuide) {
        this.name = implementationGuide.getAsJsonPrimitive(BallerinaProjectConstants.CONFIG_PROFILE_IG).getAsString();
        this.importStatement = implementationGuide.getAsJsonPrimitive("importStatement").getAsString();
        this.isEnable = implementationGuide.getAsJsonPrimitive(BallerinaProjectConstants.CONFIG_ENABLE).getAsBoolean();
        JsonArray includedProfileArray = implementationGuide.getAsJsonArray("includedProfiles");
        JsonArray excludedProfileArray = implementationGuide.getAsJsonArray("excludedProfiles");

        populateIncludedProfiles(includedProfileArray);
        populateExcludedProfiles(excludedProfileArray);
    }

    public IncludedIGConfig(TomlTable implementationGuide, String importStatement) {
        this.name = implementationGuide.getString(BallerinaProjectConstants.CONFIG_PROFILE_IG);
        this.importStatement = implementationGuide.getString("importStatement");
        Boolean enabledVal = implementationGuide.getBoolean(BallerinaProjectConstants.CONFIG_ENABLE);
        if (enabledVal != null) {
            this.isEnable = enabledVal;
        } else {
            this.isEnable = false;
        }
        TomlArray includedProfileArray = implementationGuide.getArray("included_profiles");
        TomlArray excludedProfileArray = implementationGuide.getArray("excluded_profiles");

        populateIncludedProfiles(includedProfileArray);
        populateExcludedProfiles(excludedProfileArray);
    }

    private void populateIncludedProfiles(JsonArray profiles) {
        for (int i = 0; i < profiles.size(); i++) {
            String resource = profiles.get(i).getAsJsonPrimitive().getAsString();
            includedProfiles.add(resource);
        }
    }

    private void populateIncludedProfiles(TomlArray profiles) {
        if (profiles != null) {
            for (int i = 0; i < profiles.size(); i++) {
                String resource = (String) profiles.get(i);
                includedProfiles.add(resource);
            }
        }
    }

    private void populateExcludedProfiles(JsonArray profiles) {
        if (profiles != null) {
            for (int i = 0; i < profiles.size(); i++) {
                String resource = profiles.get(i).getAsJsonPrimitive().getAsString();
                excludedProfiles.add(resource);
            }
        }
    }

    private void populateExcludedProfiles(TomlArray profiles) {
        for (int i = 0; i < profiles.size(); i++) {
            String resource = (String) profiles.get(i);
            excludedProfiles.add(resource);
        }
    }

    public String getName() {
        return name;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public List<String> getIncludedProfiles() {
        return includedProfiles;
    }

    public List<String> getExcludedProfiles() {
        return excludedProfiles;
    }

    public String getImportStatement() {
        return importStatement;
    }

    public void setImportStatement(String importStatement) {
        this.importStatement = importStatement;
    }

}
