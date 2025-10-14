package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.model;

public class FHIRResource {
    private String name;
    private String definitionUrl;
    private String ballerinaRecordUrl;
    private String resourcePackage;
    private String resourcePackageAlias;

    public FHIRResource(String name, String definitionUrl, String ballerinaRecordUrl, String resourcePackage, String resourcePackageAlias) {
        this.name = name;
        this.definitionUrl = definitionUrl;
        this.ballerinaRecordUrl = ballerinaRecordUrl;
        this.resourcePackage = resourcePackage;
        this.resourcePackageAlias = resourcePackageAlias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefinitionUrl() {
        return definitionUrl;
    }

    public void setDefinitionUrl(String definitionUrl) {
        this.definitionUrl = definitionUrl;
    }

    public String getBallerinaRecordUrl() {
        return ballerinaRecordUrl;
    }

    public void setBallerinaRecordUrl(String ballerinaRecordUrl) {
        this.ballerinaRecordUrl = ballerinaRecordUrl;
    }

    public String getResourcePackage() {
        return resourcePackage;
    }

    public void setResourcePackage(String resourcePackage) {
        this.resourcePackage = resourcePackage;
    }

    public String getResourcePackageAlias() {
        return resourcePackageAlias;
    }

    public void setResourcePackageAlias(String resourcePackageAlias) {
        this.resourcePackageAlias = resourcePackageAlias;
    }

    @Override
    public String toString() {
        return "FHIRResource{" +
                "name='" + name + '\'' +
                ", definitionUrl='" + definitionUrl + '\'' +
                ", ballerinaRecordUrl='" + ballerinaRecordUrl + '\'' +
                ", resourcePackage='" + resourcePackage + '\'' +
                ", resourcePackageAlias='" + resourcePackageAlias + '\'' +
                '}';
    }
}
