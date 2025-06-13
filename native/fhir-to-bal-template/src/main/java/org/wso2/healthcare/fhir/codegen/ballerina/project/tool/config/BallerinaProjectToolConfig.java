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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.Constants;
import org.wso2.healthcare.codegen.tool.framework.commons.config.AbstractToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.ConfigType;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main config class to hold all the config objects.
 */
public class BallerinaProjectToolConfig extends AbstractToolConfig {

    private static final Log LOG = LogFactory.getLog(BallerinaProjectToolConfig.class);
    private boolean isEnabled;
    private MetadataConfig metadataConfig;
    private String fhirVersion;
    private final Map<String, VersionConfig> versionConfigs = new HashMap<>();
    private final Map<String, IncludedIGConfig> includedIGConfigs = new HashMap<>();
    private final List<OperationConfig> operationConfig = new ArrayList<>();
    private final List<DependencyConfig> dependencyConfigs = new ArrayList<>();
    private final List<String> searchParamConfigs = new ArrayList<>();
    private final List<InteractionConfig> interactionConfigs = new ArrayList<>();

    @Override
    public void configure(ConfigType<?> configObj) throws CodeGenException {
        if (Constants.JSON_CONFIG_TYPE.equals(configObj.getType())) {
            JsonObject jsonConfigObj = ((JsonConfigType) configObj).getConfigObj();
            this.isEnabled = jsonConfigObj.getAsJsonPrimitive(BallerinaProjectConstants.CONFIG_ENABLE).getAsBoolean();
            this.metadataConfig = new MetadataConfig(jsonConfigObj.getAsJsonObject("package"));
            this.fhirVersion = jsonConfigObj.getAsJsonObject("fhir").getAsJsonPrimitive("default_version").getAsString();

            populateVersionConfig(jsonConfigObj.getAsJsonObject("fhir").getAsJsonArray("versionConfigs"));
            populateIgConfigs(jsonConfigObj.getAsJsonArray("includedIGs"));
            populateOperationConfigs(jsonConfigObj.getAsJsonObject("builtIn").getAsJsonArray("operations"));
            populateSearchParamConfigs(jsonConfigObj.getAsJsonObject("builtIn").getAsJsonArray("searchParams"));
            populateDependencyConfigs(jsonConfigObj.getAsJsonArray("dependencies"));
            populateInteractionConfigs(jsonConfigObj.getAsJsonObject("builtIn").getAsJsonArray("interactions"));
        }
        //todo: add toml type config handling
    }

    @Override
    public void overrideConfig(String jsonPath, JsonElement value) {
        switch (jsonPath) {
            case "project.package.org":
                this.metadataConfig.setOrg(value.getAsString());
                break;
            case "project.package.version":
                this.metadataConfig.setVersion(value.getAsString());
                break;
            case "project.fhir.default_version":
                this.fhirVersion = value.getAsString();
                break;
            case "project.package.namePrefix":
                this.versionConfigs.get(fhirVersion).setNamePrefix(value.getAsString());
                break;
            case "project.package.dependentPackage":
                this.versionConfigs.get(fhirVersion).setDependentPackage(value.getAsString());
                break;
            case "project.package.igConfig":
                this.includedIGConfigs.put(
                        value.getAsJsonObject().getAsJsonPrimitive("implementationGuide").getAsString(),
                        new IncludedIGConfig(value.getAsJsonObject()));
                break;
            default:
                LOG.warn("Invalid config path: " + jsonPath);
        }
    }

    private void populateVersionConfig(JsonArray versionArray) {
        if(versionArray != null){
            for (int i=0; i<versionArray.size(); i++){
                this.versionConfigs.putIfAbsent(
                    versionArray.get(i).getAsJsonObject().getAsJsonPrimitive("fhirVersion").getAsString(),
                    new VersionConfig(versionArray.get(i).getAsJsonObject())
                );
            }
        }
    }

    private void populateIgConfigs(JsonArray igArray) {
        if (igArray != null) {
            for (int i = 0; i < igArray.size(); i++) {
                includedIGConfigs.put(
                        igArray.get(i).getAsJsonObject().getAsJsonPrimitive("implementationGuide").getAsString(),
                        new IncludedIGConfig(igArray.get(i).getAsJsonObject()));
            }
        }
    }

    private void populateOperationConfigs(JsonArray opsArray) {
        for (int i = 0; i < opsArray.size(); i++) {
            //todo: populate operations from FHIR spec
            operationConfig.add(new OperationConfig(opsArray.get(i).getAsJsonPrimitive().getAsString()));
        }
    }

    private void populateSearchParamConfigs(JsonArray paramArray) {
        for (int i = 0; i < paramArray.size(); i++) {
            //todo: populate search params from FHIR spec
            searchParamConfigs.add(paramArray.get(i).getAsJsonPrimitive().getAsString());
        }
    }

    private void populateDependencyConfigs(JsonArray igArray) {
        for (int i = 0; i < igArray.size(); i++) {
            dependencyConfigs.add(new DependencyConfig(igArray.get(i).getAsJsonObject()));
        }
    }

    private void populateInteractionConfigs(JsonArray igArray) {
        for (int i = 0; i < igArray.size(); i++) {
            if (igArray.get(i).getAsJsonObject().getAsJsonPrimitive("enable").getAsBoolean()) {
                interactionConfigs.add(new InteractionConfig(igArray.get(i).getAsJsonObject()));
            }
        }
    }

    public MetadataConfig getMetadataConfig() {
        return metadataConfig;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public List<DependencyConfig> getDependencyConfig() {
        return dependencyConfigs;
    }

    public List<OperationConfig> getOperationConfig() {
        return operationConfig;
    }

    public Map<String, IncludedIGConfig> getIncludedIGConfigs() {
        return includedIGConfigs;
    }

    public List<DependencyConfig> getDependencyConfigs() {
        return dependencyConfigs;
    }

    public List<String> getSearchParamConfigs() {
        return searchParamConfigs;
    }

    public List<InteractionConfig> getInteractionConfigs() {
        return interactionConfigs;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public VersionConfig getVersionConfig() {
        return versionConfigs.get(this.fhirVersion);
    }
}
