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

import com.google.gson.JsonObject;
import org.apache.commons.text.CaseUtils;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;

import java.util.*;

public class FHIRProfile {

    private StructureDefinition profileDef;
    private String parentRef;
    private boolean isAbstract;
    private String name;
    private String fhirVersion;
    private String url;
    private Set<String> importsList;
    private Map<String, JsonObject> examples = new HashMap<>();
    private String igName;
    private String resourceType;

    private String packagePrefix;

    public FHIRProfile(StructureDefinition profileDef, String url, String igName, String resourceType) {
        this.igName = igName;
        this.resourceType = resourceType;
        isAbstract = false;
        this.profileDef = profileDef;
        this.url = url;
        this.importsList = new HashSet<>();
        this.name = profileDef.getName();
    }

    public StructureDefinition getProfileDef() {
        return profileDef;
    }

    public void setProfileDef(StructureDefinition profileDef) {
        this.profileDef = profileDef;
    }

    public String getParentRef() {
        return parentRef;
    }

    public void setParentRef(String parentRef) {
        this.parentRef = parentRef;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract() {
        isAbstract = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getNamePrefix(){
        return CaseUtils.toCamelCase(igName, true, '_') + CaseUtils.toCamelCase(resourceType, true, '_');
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public void setFhirVersion(String fhirVersion) {
        this.fhirVersion = fhirVersion;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<String> getImportsList() {
        return importsList;
    }

    public void addImport(String importItem) {
        this.importsList.add(importItem);
    }

    public void addExample(String interaction, JsonObject example){
        examples.put(interaction,example);
    }

    public Map<String,JsonObject> getExamples(){
        return examples;
    }

    public String getIgName() {
        return igName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(BallerinaProjectToolConfig config) {

        String importStatementWithoutOrg = config.getIncludedIGConfigs().get(
                igName).getImportStatement().split("/")[1];
        System.out.println(importStatementWithoutOrg);
        this.packagePrefix = importStatementWithoutOrg.split(
                "\\.")[importStatementWithoutOrg.split("\\.").length- 1];
    }
}
