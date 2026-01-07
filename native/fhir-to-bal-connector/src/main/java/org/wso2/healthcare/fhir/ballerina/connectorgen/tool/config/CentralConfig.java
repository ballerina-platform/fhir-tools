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

import java.util.List;

public class CentralConfig {

    private String url;
    private String orgName;
    private List<String> profilePackages;

    public CentralConfig(JsonObject centralConfigJson) {
        this.url = centralConfigJson.getAsJsonPrimitive("url").getAsString();
        this.orgName = centralConfigJson.getAsJsonPrimitive("orgName").getAsString();
        this.profilePackages = centralConfigJson.getAsJsonArray("profilePackages").asList().stream()
                .map(JsonElement::getAsString).toList();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public List<String> getProfilePackages() {
        return profilePackages;
    }

    public void setProfilePackages(List<String> profilePackages) {
        this.profilePackages = profilePackages;
    }



}
