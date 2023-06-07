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
 * Ballerina Keywords related config.
 */
public class BallerinaKeywordConfig {
    private final String keyword;
    private final String replace;

    public BallerinaKeywordConfig(JsonObject ballerinaKeywordConfigJson) {
        this.keyword = ballerinaKeywordConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_BALLERINA_KEYWORD_KEYWORD).getAsString();
        this.replace = ballerinaKeywordConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_BALLERINA_KEYWORD_REPLACE).getAsString();
    }

    public BallerinaKeywordConfig(TomlTable ballerinaKeywordConfigToml) {
        this.keyword = ballerinaKeywordConfigToml.getString(ToolConstants.CONFIG_BALLERINA_KEYWORD_KEYWORD_TOML);
        this.replace = ballerinaKeywordConfigToml.getString(ToolConstants.CONFIG_BALLERINA_KEYWORD_REPLACE_TOML);
    }

    public String getKeyword() {
        return keyword;
    }

    public String getReplace() {
        return replace;
    }
}
