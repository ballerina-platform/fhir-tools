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

package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.config.AbstractToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.model.ConfigType;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;

public class BallerinaConnectorGenToolConfig extends AbstractToolConfig {

    private static final Log LOG = LogFactory.getLog(BallerinaConnectorGenToolConfig.class);
    private CentralConfig centralConfig;
    private String fhirServerUrl;

    @Override
    public void configure(ConfigType<?> configObj) {
        LOG.debug("Started: Ballerina Connector Generator Tool config population");
        JsonObject jsonConfigObj = ((JsonConfigType) configObj).getConfigObj();
        this.centralConfig = new CentralConfig(jsonConfigObj.getAsJsonObject("central"));
        this.fhirServerUrl = jsonConfigObj.get("fhirServerUrl").getAsString();
        LOG.debug("Ended: Ballerina Connector Generator Tool config population");
    }

    @Override
    public void overrideConfig(String s, JsonElement jsonElement) {
        // No implementation needed as of now
    }

    public CentralConfig getCentralConfig() {
        return centralConfig;
    }

    public void setCentralConfig(CentralConfig centralConfig) {
        this.centralConfig = centralConfig;
    }

    public String getFhirServerUrl() {
        return fhirServerUrl;
    }

    public void setFhirServerUrl(String fhirServerUrl) {
        this.fhirServerUrl = fhirServerUrl;
    }
}
