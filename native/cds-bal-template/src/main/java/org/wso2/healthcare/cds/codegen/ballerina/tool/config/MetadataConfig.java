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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.*;

public class MetadataConfig {

    private String org;
    private String namePrefix;
    private String version;
    private String distribution;
    private final List<String> authors = new ArrayList<>();
    private final List<String> keywords = new ArrayList<>();

    public MetadataConfig(JsonObject packageConfig) {
        //todo: add null checks
        if (packageConfig.has(ORG)) {
            this.org = packageConfig.getAsJsonPrimitive(ORG).getAsString();
        }

        if (packageConfig.has(NAME_PREFIX)) {
            this.namePrefix = packageConfig.getAsJsonPrimitive(NAME_PREFIX).getAsString();
        }

        if (packageConfig.has(VERSION)) {
            this.version = packageConfig.getAsJsonPrimitive(VERSION).getAsString();
        }

        if (packageConfig.has(DISTRIBUTION)) {
            this.distribution = packageConfig.getAsJsonPrimitive(DISTRIBUTION).getAsString();
        }

        if (packageConfig.has(AUTHORS)) {
            for (JsonElement authorElem : packageConfig.getAsJsonArray(AUTHORS).getAsJsonArray()) {
                authors.add(authorElem.getAsString());
            }
        }

        if (packageConfig.has(KEYWORDS)) {
            for (JsonElement authorElem : packageConfig.getAsJsonArray(KEYWORDS).getAsJsonArray()) {
                keywords.add(authorElem.getAsString());
            }
        }
    }

    public String getDistribution() {
        return distribution;
    }

    public String getOrg() {
        return org;
    }

    public String getNamePrefix() {
        return namePrefix;
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
        // org name of a ballerina package can have only alphanumeric chars and '_'
        // it cannot have consecutive '_'
        // and it cannot start/end with '_'
        String normalizedOrg = org.replaceAll(REGEX_STRING_FOR_NON_WORD_CHARACTER, UNDERSCORE);
        if (normalizedOrg.contains(UNDERSCORE + UNDERSCORE)) {
            normalizedOrg = normalizedOrg.replaceAll("_{2,}", UNDERSCORE);
        }
        if (normalizedOrg.startsWith(UNDERSCORE)) {
            normalizedOrg = normalizedOrg.substring(ONE);
        }
        if (normalizedOrg.endsWith(UNDERSCORE)) {
            normalizedOrg = normalizedOrg.substring(ZERO, normalizedOrg.length() - ONE);
        }
        this.org = normalizedOrg;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }
}
