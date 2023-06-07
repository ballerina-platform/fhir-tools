/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.InteractionConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.util.BallerinaProjectUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneratedUtil {

    private String resourceType;
    private String fhirVersion;
    private String defaultProfile;
    private List<String> importsList;
    private List<SetterMethod> setterMethods;
    private List<ImplFunction> implFunctions;
    private Map<String,String> interactionImpl;

    public GeneratedUtil(String resourceName, String fhirVersion){
        this.resourceType = resourceName;
        this.fhirVersion = fhirVersion;
        importsList = new ArrayList<>();
        setterMethods = new ArrayList<>();
        implFunctions = new ArrayList<>();
        interactionImpl = new HashMap<>();
    }

    public void addImport(String importPackage) {
        importsList.add(importPackage);
    }

    public void addSetterMethod(SetterMethod setterMethod) {
        setterMethods.add(setterMethod);
    }

    public void addGetterMethod(ImplFunction implFunction) {
        implFunctions.add(implFunction);
    }

    public List<ImplFunction> getImplFunctions() {
        return implFunctions;
    }

    public String getResourceType() {
        return resourceType;
    }

    public List<String> getImportsList() {
        return importsList;
    }

    public List<SetterMethod> getSetterMethods() {
        return setterMethods;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public void setInteractionImpl(BallerinaProjectToolConfig config){
        interactionImpl = getImplSignatures(config);
    }

    public void addInteractionImpl(String interactionName, String implSignature){
        interactionImpl.put(interactionName, implSignature);
    }

    public Map<String, String> getInteractionImpl() {
        return interactionImpl;
    }

    public String getDefaultProfile() {
        return defaultProfile;
    }

    public void setDefaultProfile(String defaultProfile) {
        this.defaultProfile = defaultProfile;
    }

    private Map<String, String> getImplSignatures(BallerinaProjectToolConfig config){
        Map<String,String> implFunctions = new HashMap<>();

        for (InteractionConfig interactionConfig: config.getInteractionConfigs()) {
            List<String> imports = new ArrayList<>();
            imports.add(resourceType.toLowerCase());
            imports.add(interactionConfig.getName());
            imports.add("impl");
            implFunctions.put(interactionConfig.getName(), BallerinaProjectUtil.aggregateCamelcase(imports));
        }
        return implFunctions;
    }

    public void addImplFunction(ImplFunction implFunction) {
        implFunctions.add(implFunction);
    }
}
