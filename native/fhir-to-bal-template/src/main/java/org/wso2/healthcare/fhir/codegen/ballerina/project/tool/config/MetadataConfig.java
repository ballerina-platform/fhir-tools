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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MetadataConfig {

    private String org;
    private String version;
    private String distribution;
    private final List<String> authors = new ArrayList<>();
    private final List<String> keywords = new ArrayList<>();

    public MetadataConfig(JsonObject packageConfig) {
        //todo: add null checks
        this.org = packageConfig.getAsJsonPrimitive("org").getAsString();
        this.version = packageConfig.getAsJsonPrimitive("version").getAsString();
        this.distribution = packageConfig.getAsJsonPrimitive("distribution").getAsString();
        for (JsonElement authorElem : packageConfig.getAsJsonArray("authors").getAsJsonArray()) {
            authors.add(authorElem.getAsString());
        }
        for (JsonElement authorElem : packageConfig.getAsJsonArray("keywords").getAsJsonArray()) {
            keywords.add(authorElem.getAsString());
        }
    }

    public String getDistribution() {
        return distribution;
    }

    public String getOrg() {
        return org;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setOrg(String org) {
        // org name of a ballerina package can have only alhpa-numeric chars and '_'
        // it cannot have consecutive '_'
        // and it cannot start/end with '_'
        String normalizedOrg = org.replaceAll("[^a-zA-Z0-9_]", "_");
        if (normalizedOrg.contains("__")) {
            normalizedOrg = normalizedOrg.replaceAll("_{2,}", "_");
        }
        if (normalizedOrg.startsWith("_")) {
            normalizedOrg = normalizedOrg.substring(1);
        }
        if (normalizedOrg.endsWith("_")) {
            normalizedOrg = normalizedOrg.substring(0, normalizedOrg.length() - 1);
        }
        this.org = normalizedOrg;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }
}
