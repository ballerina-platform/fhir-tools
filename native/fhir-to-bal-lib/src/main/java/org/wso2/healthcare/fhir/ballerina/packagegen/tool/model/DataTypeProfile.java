package org.wso2.healthcare.fhir.ballerina.packagegen.tool.model;

/**
 * Model for FHIR resource Data Type Profile.
 */

public class DataTypeProfile {
    private String identifier;
    private String profileType;
    private String prefix;

    public DataTypeProfile(String identifier, String profileType) {
        this.identifier = identifier;
        this.profileType = profileType;
    }

    public String getIdentifier() {
        return identifier;
    }
    public String getProfileType() {
        return profileType;
    }

    public void setProfileType(String profileType) {
        this.profileType = profileType;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
