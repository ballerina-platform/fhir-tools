/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.cds.codegen.ballerina.tool.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.cds.codegen.ballerina.tool.model.CdsHook;
import org.wso2.healthcare.codegen.tool.framework.commons.Constants;
import org.wso2.healthcare.codegen.tool.framework.commons.config.AbstractToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.ConfigType;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;

import java.util.ArrayList;
import java.util.List;

import static org.wso2.healthcare.cds.codegen.ballerina.tool.BallerinaCDSProjectConstants.*;


/**
 * Main config class to hold all the config objects.
 */
public class BallerinaCDSProjectToolConfig extends AbstractToolConfig {

    private static final Log LOG = LogFactory.getLog(BallerinaCDSProjectToolConfig.class);
    private MetadataConfig metadataConfig;
    private final List<DependencyConfig> dependencyConfigs = new ArrayList<>();
    private String basePackage;
    private String dependentPackage;
    private final List<CdsHook> cdsHooks = new ArrayList<>();

    @Override
    public void configure(ConfigType<?> configObj) throws CodeGenException {
        if (Constants.JSON_CONFIG_TYPE.equals(configObj.getType())) {
            JsonObject jsonConfigObj = ((JsonConfigType) configObj).getConfigObj();
            jsonConfigObj = jsonConfigObj.getAsJsonObject(CONFIG);

            this.metadataConfig = new MetadataConfig(jsonConfigObj.getAsJsonObject(PACKAGE));
            populateDependencyConfigs(jsonConfigObj.
                    getAsJsonArray(DEPENDENCIES));

            if(jsonConfigObj.getAsJsonPrimitive((BASE_PACKAGE)) != null){
                this.basePackage = jsonConfigObj
                        .getAsJsonPrimitive(BASE_PACKAGE).getAsString();
            }

            if (jsonConfigObj.getAsJsonPrimitive(DEPENDENT_PACKAGE) != null) {
                this.dependentPackage = jsonConfigObj
                        .getAsJsonPrimitive(DEPENDENT_PACKAGE).getAsString();
            }

            jsonConfigObj = ((JsonConfigType) configObj).getConfigObj().getAsJsonObject(HOOKS);
            loadCdsHooks(jsonConfigObj);
        }
    }

    public void loadCdsHooks(JsonObject cdsHookConfig) {
        if (cdsHookConfig.get(CDS_SERVICES) instanceof JsonArray) {
            cdsHookConfig.getAsJsonArray(CDS_SERVICES).forEach(a -> {
                cdsHooks.add(new CdsHook(a.getAsJsonObject()));
            });
        }
    }

    @Override
    public void overrideConfig(String jsonPath, JsonElement value) {

        switch (jsonPath) {
            case PROJECT_PACKAGE_ORG:
                this.metadataConfig.setOrg(value.getAsString());
                break;
            case PROJECT_PACKAGE_VERSION:
                this.metadataConfig.setVersion(value.getAsString());
                break;
            case PROJECT_PACKAGE_NAME_PREFIX:
                this.metadataConfig.setNamePrefix(value.getAsString());
                break;
            case PROJECT_PACKAGE_BASE_PACKAGE:
                this.basePackage = value.getAsString();
                break;
            case PROJECT_PACKAGE_DEPENDENT_PACKAGE:
                this.dependentPackage = value.getAsString();
                break;
            default:
                LOG.warn("Invalid config path: " + jsonPath);
        }
    }

    private void populateDependencyConfigs(JsonArray igArray) {
        for (int i = 0; i < igArray.size(); i++) {
            dependencyConfigs.add(new DependencyConfig(igArray.get(i).getAsJsonObject()));
        }
    }

    public MetadataConfig getMetadataConfig() {
        return metadataConfig;
    }

    public List<DependencyConfig> getDependencyConfig() {
        return dependencyConfigs;
    }

    public List<DependencyConfig> getDependencyConfigs() {
        return dependencyConfigs;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public String getDependentPackage() {
        return dependentPackage;
    }

    public List<CdsHook> getCdsHooks() {
        return cdsHooks;
    }
}
