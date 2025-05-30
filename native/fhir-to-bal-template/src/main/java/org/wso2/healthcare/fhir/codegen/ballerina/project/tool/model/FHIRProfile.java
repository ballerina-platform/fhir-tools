package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model;

import com.google.gson.JsonObject;
import org.apache.commons.text.CaseUtils;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FHIRProfile <S>{
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

    public FHIRProfile(S profileDef, String url, String igName, String resourceType) {
        this.igName = igName;
        this.resourceType = resourceType;
        isAbstract = false;
        this.url = url;
        this.importsList = new HashSet<>();
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
        String igPackage = config.getDependentPackage();
        if (igPackage.contains("/")) {
            String pkgNameWithoutOrg = igPackage.split("/")[1];
            this.packagePrefix = pkgNameWithoutOrg.substring(pkgNameWithoutOrg.lastIndexOf(".") + 1);
        } else {
            this.packagePrefix = igPackage.substring(igPackage.lastIndexOf(".") + 1);
        }
    }
}
