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

package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource {
    private String type;
    private List<String> supportedProfile;
    private String documentation;
    private List<Interaction> interaction;
    private List<SearchParam> searchParam;
    private List<String> searchInclude;
    private List<String> searchRevInclude;
    private List<Operation> operation;


    // getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getSupportedProfile() {
        return supportedProfile;
    }

    public void setSupportedProfile(List<String> supportedProfile) {
        this.supportedProfile = supportedProfile;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public List<Interaction> getInteraction() {
        return interaction;
    }

    public void setInteraction(List<Interaction> interaction) {
        this.interaction = interaction;
    }

    public List<SearchParam> getSearchParam() {
        return searchParam;
    }

    public void setSearchParam(List<SearchParam> searchParam) {
        this.searchParam = searchParam;
    }

    public List<String> getSearchInclude() {
        return searchInclude;
    }

    public void setSearchInclude(List<String> searchInclude) {
        this.searchInclude = searchInclude;
    }

    public List<String> getSearchRevInclude() {
        return searchRevInclude;
    }

    public void setSearchRevInclude(List<String> searchRevInclude) {
        this.searchRevInclude = searchRevInclude;
    }

    public List<Operation> getOperation() {
        return operation;
    }

    public void setOperation(List<Operation> operation) {
        this.operation = operation;
    }
}
