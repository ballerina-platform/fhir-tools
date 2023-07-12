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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.model;

import java.util.Map;

/**
 * Class holder for IG related template context
 */
public class IGTemplateContext {
    private String title;
    private String igName;
    private String igCode;
    private String baseIgName;
    private Map<String, Map<String, SearchParameter>> searchParameters;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIgName() {
        return igName;
    }

    public void setIgName(String igName) {
        this.igName = igName;
    }

    public String getBaseIgName() {
        return baseIgName;
    }

    public void setBaseIgName(String baseIgName) {
        this.baseIgName = baseIgName;
    }

    public String getIgCode() {
        return igCode;
    }

    public void setIgCode(String igCode) {
        this.igCode = igCode;
    }

    public Map<String, Map<String, SearchParameter>> getSearchParameters() {
        return searchParameters;
    }

    public void setSearchParameters(Map<String, Map<String, SearchParameter>> searchParameters) {
        this.searchParameters = searchParameters;
    }
}
