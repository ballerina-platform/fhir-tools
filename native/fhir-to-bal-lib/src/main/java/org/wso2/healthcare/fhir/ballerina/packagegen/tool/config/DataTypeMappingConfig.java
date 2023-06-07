// Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.config;

import com.google.gson.JsonObject;
import net.consensys.cava.toml.TomlTable;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;

/**
 * Data type mapping related config.
 */
public class DataTypeMappingConfig {
    private final String fhirType;
    private final String ballerinaType;

    public DataTypeMappingConfig(JsonObject implementationGuide) {
        this.fhirType = implementationGuide.getAsJsonPrimitive(ToolConstants.CONFIG_DATA_TYPE_FHIR).getAsString();
        this.ballerinaType = implementationGuide.getAsJsonPrimitive(ToolConstants.CONFIG_DATA_TYPE_BALLERINA).getAsString();
    }

    public DataTypeMappingConfig(TomlTable implementationGuide) {
        this.fhirType = implementationGuide.getString(ToolConstants.CONFIG_DATA_TYPE_FHIR_TOML);
        this.ballerinaType = implementationGuide.getString(ToolConstants.CONFIG_DATA_TYPE_BALLERINA_TOML);
    }

    public String getFhirType() {
        return fhirType;
    }

    public String getBallerinaType() {
        return ballerinaType;
    }
}
