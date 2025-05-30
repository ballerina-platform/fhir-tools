package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.versions.r5;

import com.google.gson.JsonObject;
import org.apache.commons.text.CaseUtils;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.FHIRProfile;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.util.BallerinaProjectUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class R5FHIRProfile extends FHIRProfile<StructureDefinition> {
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

    public R5FHIRProfile(StructureDefinition profileDef, String url, String igName, String resourceType) {
        super(profileDef, url, igName, resourceType);
        this.profileDef = profileDef;
        setName(BallerinaProjectUtil.resolveSpecialCharacters(profileDef.getName()));
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
        String igPackage = config.getDependentPackage();
        if (igPackage.contains("/")) {
            String pkgNameWithoutOrg = igPackage.split("/")[1];
            this.packagePrefix = pkgNameWithoutOrg.substring(pkgNameWithoutOrg.lastIndexOf(".") + 1);
        } else {
            this.packagePrefix = igPackage.substring(igPackage.lastIndexOf(".") + 1);
        }
    }
}
