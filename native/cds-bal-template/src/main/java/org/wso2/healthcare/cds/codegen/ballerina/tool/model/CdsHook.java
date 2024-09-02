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

package org.wso2.healthcare.cds.codegen.ballerina.tool.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.healthcare.cds.codegen.ballerina.tool.BallerinaCDSProjectConstants.DESCRIPTION;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.BallerinaCDSProjectConstants.HOOK;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.BallerinaCDSProjectConstants.ID;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.BallerinaCDSProjectConstants.PREFETCH;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.BallerinaCDSProjectConstants.TITLE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.BallerinaCDSProjectConstants.USAGE_REQUIREMENTS;


public class CdsHook {
    private String id;
    private HookType hook;
    private String title;
    private String description;
    private Map<String, String> prefetch;
    private String usageRequirements;

    public CdsHook() {
    }

    public CdsHook(JsonObject config) {
        this.id = config.get(ID).getAsString();
        this.hook = HookType.fromString(config.get(HOOK).getAsString());
        this.title = config.get(TITLE).getAsString();
        if (config.has(DESCRIPTION)) {
            this.description = config.get(DESCRIPTION).getAsString();
        }

        if (config.has(PREFETCH)) {
            JsonObject prefetch = config.getAsJsonObject(PREFETCH);
            this.prefetch = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : prefetch.asMap().entrySet()) {
                this.prefetch.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        if (config.has(USAGE_REQUIREMENTS)) {
            this.usageRequirements = config.get(USAGE_REQUIREMENTS).getAsString();
        }
    }

    public CdsHook(String id, HookType hook, String title, String description, Map<String, String> prefetch) {
        this.id = id;
        this.hook = hook;
        this.title = title;
        this.description = description;
        this.prefetch = prefetch;
    }

    public CdsHook(String id, HookType hook, String title, String description) {
        this.id = id;
        this.hook = hook;
        this.title = title;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHook() {
        return hook.getValue();
    }

    public void setHook(HookType hook) {
        this.hook = hook;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getPrefetch() {
        return prefetch;
    }

    public void setPrefetch(Map<String, String> prefetch) {
        this.prefetch = prefetch;
    }

    public String getUsageRequirements() {
        return usageRequirements;
    }

    public void setUsageRequirements(String usageRequirements) {
        this.usageRequirements = usageRequirements;
    }
}
