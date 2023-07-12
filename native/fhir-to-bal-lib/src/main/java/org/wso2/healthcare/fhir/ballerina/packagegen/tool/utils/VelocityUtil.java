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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils;

import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Utility class for velocity templates
 */
public class VelocityUtil {
    private final BallerinaPackageGenToolConfig toolConfig;
    private final HashMap<String, String> SPECIAL_CHARACTERS_MAP = new HashMap<String, String>() {{
        put("=", "equal");
        put("!=", "not_equal");
        put(">", "greater_than");
        put(">=", "greater_than_or_equal");
        put("<", "less_than");
        put("<=", "less_than_or_equal");
    }};

    private final HashMap<String, String> KEYWORD_CONFLICTS_MAP = new HashMap<String, String>() {{
        put("type", "'type");
        put("source", "'source");
        put("client", "'client");
        put("resource", "'resource");
        put("order", "'order");
        put("class", "'class");
        put("version", "'version");
        put("final", "'final");
        put("error", "'error");
        put("parameter", "'parameter");
        put("start", "'start");
        put("transaction", "'transaction");
        put("json", "_json");
        put("service", "'service");
        put("function", "'function");
        put("fail", "'fail");
        put("in", "'in");
        put("abstract", "'abstract");
        put("import", "'import");
        put("string", "_string");
        put("from", "'from");
        put("boolean", "'boolean");
        put("outer", "'outer");
        put("never", "'never");
        put("on", "'on");
        put("decimal", "'decimal");
        put("limit", "'limit");
        put("check", "'check");
        put("field", "'field");
    }};

    public VelocityUtil(BallerinaPackageGenToolConfig config) {
        this.toolConfig = config;
    }

    /**
     * Resolve keyword conflicts of Ballerina
     *
     * @param keyword Ballerina keyword causes the conflict
     * @return replacement from tool configs
     */
    public String resolveKeywordConflict(String keyword) {
        if (this.KEYWORD_CONFLICTS_MAP.containsKey(keyword)) {
            return this.KEYWORD_CONFLICTS_MAP.get(keyword);
        } else if (this.toolConfig.getBallerinaKeywordConfig().containsKey(keyword)) {
            return this.toolConfig.getBallerinaKeywordConfig().get(keyword).getReplace();
        }
        return keyword;
    }

    /**
     * Resolve for special character
     *
     * @param specialChar special character
     * @return preferred string replacement
     */
    public String resolveSpecialCharacters(String specialChar) {
        if (this.SPECIAL_CHARACTERS_MAP.containsKey(specialChar))
            return this.SPECIAL_CHARACTERS_MAP.get(specialChar);
        return specialChar.replaceAll(Pattern.quote("-"), "_").replaceAll(Pattern.quote("[x]"), "");
    }


    /**
     * Getter for newline in templates
     *
     * @return newline
     */
    public String getNewLine() {
        return "\n";
    }
}
