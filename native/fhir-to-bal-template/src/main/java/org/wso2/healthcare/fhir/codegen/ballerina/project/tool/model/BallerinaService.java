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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model;

import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.OperationConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Ballerina service model
 */
public class BallerinaService {
    private String name;
    private String fhirVersion;
    private List<String> importsList;
    private List<String> interceptorsList;
    private List<String> profiles;
    //todo: remove this and use the latest Profilelist
    private List<FHIRProfile> profileList;

    private List<OperationConfig> operationConfigs;
    private List<SearchParam> searchParamConfigs;
    private List<ResourceMethod> resourceMethods;
    private List<String> igs;

    public BallerinaService(String name, String fhirVersion) {
        this.name = name;
        this.fhirVersion = fhirVersion;
        importsList = new ArrayList<>();
        interceptorsList = new ArrayList<>();
        resourceMethods = new ArrayList<>();
        profiles = new ArrayList<>();
        searchParamConfigs = new ArrayList<>();
        igs = new ArrayList<>();
        profileList = new ArrayList<>();
    }

    public String getName() {
        return name;
    }
    public String getNameLowerCase() {
        return name.toLowerCase();
    }

    public void addImport(String importPackage) {
        importsList.add(importPackage);
    }

    public void addInterceptor(String interceptor) {
        interceptorsList.add(interceptor);
    }

    public void addProfile(String profile) {
        profiles.add(profile);
    }

    public void addSearchParam(SearchParam param) {
        searchParamConfigs.add(param);
    }

    public void addResourceMethod(ResourceMethod resourceMethod) {
        resourceMethods.add(resourceMethod);
    }

    public List<String> getImportsList() {
        return importsList;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public List<String> getInterceptorsList() {
        return interceptorsList;
    }

    public List<OperationConfig> getOperationConfigs() {
        return operationConfigs;
    }

    public void setOperationConfigs(List<OperationConfig> operationConfigs) {
        this.operationConfigs = operationConfigs;
    }

    public List<SearchParam> getSearchParamConfigs() {
        return searchParamConfigs;
    }

    public List<ResourceMethod> getResourceMethods() {
        return resourceMethods;
    }

    public List<String> getProfiles() {
        return profiles;
    }
    public void addIg(String igName){
        igs.add(igName);
    }

    public List<String> getIgs() {
        return igs;
    }

    public List<FHIRProfile> getProfileList() {
        return profileList;
    }

    public void addFhirProfile(FHIRProfile profile){
        profileList.add(profile);
    }
}
