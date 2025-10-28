package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.config;

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
                .map(e -> e.getAsString()).toList();
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
