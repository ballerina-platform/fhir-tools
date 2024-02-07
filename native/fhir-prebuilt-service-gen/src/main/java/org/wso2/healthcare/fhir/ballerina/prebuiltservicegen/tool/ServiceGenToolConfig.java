/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.healthcare.fhir.ballerina.prebuiltservicegen.tool;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.wso2.healthcare.codegen.tool.framework.commons.Constants;
import org.wso2.healthcare.codegen.tool.framework.commons.config.AbstractToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.ConfigType;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;
import org.wso2.healthcare.fhir.ballerina.prebuiltservicegen.tool.config.PackageInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the configuration for the FHIR prebuilt service gen tool.
 */
public class ServiceGenToolConfig extends AbstractToolConfig {

    private static final Log LOG = LogFactory.getLog(ServiceGenToolConfig.class);
    private boolean isEnabled;
    private String fhirServerName;
    private String projectName;
    private IBaseResource capabilityStatement;
    private String authMethod;
    private final Map<String, PackageInfo> packageInfoMap = new HashMap<>();

    @Override
    public void configure(ConfigType<?> configObj) throws CodeGenException {
        if (Constants.JSON_CONFIG_TYPE.equals(configObj.getType())) {
            JsonObject jsonConfigObj = ((JsonConfigType) configObj).getConfigObj();
            this.isEnabled = jsonConfigObj.getAsJsonPrimitive(ToolConstants.CONFIG_ENABLE).getAsBoolean();
            jsonConfigObj.getAsJsonObject("igPackageInfo").entrySet().forEach(entry -> {
                String key = entry.getKey();
                JsonObject value = entry.getValue().getAsJsonObject();
                PackageInfo packageInfo = new PackageInfo(value.get("packagePrefix").getAsString(),
                        value.get("importStatement").getAsString());
                packageInfoMap.putIfAbsent(key, packageInfo);
            });
        }
    }

    @Override
    public void overrideConfig(String jsonPath, JsonElement jsonElement) {
        switch (jsonPath) {
            case "servicegen.config.projectName":
                this.projectName = jsonElement.getAsString();
                break;
            case "servicegen.config.serverName":
                this.fhirServerName = jsonElement.getAsString();
                break;
            case "servicegen.config.authMethod":
                this.authMethod = jsonElement.getAsString();
                break;
            default:
                LOG.warn("Invalid config path: " + jsonPath);
                break;
        }
    }

    public String getFhirServerName() {
        return fhirServerName;
    }

    public String getProjectName() {
        if (projectName == null) {
            projectName = fhirServerName.concat("-service");
        }
        return projectName;
    }

    public IBaseResource getCapabilityStatement() {
        return capabilityStatement;
    }

    public void setCapabilityStatement(IBaseResource capabilityStatement) {
        this.capabilityStatement = capabilityStatement;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public Map<String, PackageInfo> getPackageInfoMap() {
        return packageInfoMap;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
