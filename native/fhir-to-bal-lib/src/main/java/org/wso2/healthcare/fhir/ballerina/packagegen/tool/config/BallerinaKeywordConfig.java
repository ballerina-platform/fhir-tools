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
