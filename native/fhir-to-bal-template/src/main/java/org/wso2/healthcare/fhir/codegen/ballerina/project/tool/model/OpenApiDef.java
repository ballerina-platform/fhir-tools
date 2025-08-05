package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.oas.OASGenUtils;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import static org.wso2.healthcare.codegen.tool.framework.fhir.core.oas.APIDefinitionConstants.OAS_EXTENSION_OH_FHIR_PROFILE;
import static org.wso2.healthcare.codegen.tool.framework.fhir.core.oas.APIDefinitionConstants.OAS_EXTENSION_OH_FHIR_RESOURCE_TYPE;

public class OpenApiDef {
    private static OpenAPI openAPI;
    private static Info infoFields;
    private static Set<String> resourceTypes;
    private static Set<String> supportedProfiles;
    private static Set<Tag> tags;
    private static Map<String, Paths> pathsMap;
    private static Components components;

    private OpenApiDef() {
        openAPI = new OpenAPI();
        infoFields = null;
        resourceTypes = new HashSet<>();
        supportedProfiles = new HashSet<>();
        tags = new HashSet<>();
        pathsMap = new HashMap<>();
        components = new Components();
    }

    private static final OpenApiDef OPEN_API_DEF_INSTANCE = new OpenApiDef();

    public static OpenApiDef getInstance() {
        return OPEN_API_DEF_INSTANCE;
    }

    public void setInfoFields(Info infoFields) {
        OpenApiDef.infoFields = infoFields;
    }

    public Set<String> getResourceTypes() {
        return resourceTypes;
    }

    public Set<String> getSupportedProfiles() {
        return supportedProfiles;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public Map<String, Paths> getPathsMap() {
        return pathsMap;
    }

    public void setComponents(Components components) {
        OpenApiDef.components = components;
    }

    /**
     * Creates a new OpenAPI definition based on the aggregated resources.
     *
     * @return OpenAPI object containing the aggregated definitions.
     * @throws NullPointerException if any required field is null.
     */
    public static OpenAPI createNewOpenAPIDef() throws NullPointerException {
        String concatenatedTitle = "OAS Definition for " + String.join(", ", resourceTypes);
        String description = OASGenUtils.generateDescription("multiple", supportedProfiles);
        String summary = infoFields.getSummary();
        String termsOfService = infoFields.getTermsOfService();
        Contact contact = infoFields.getContact();
        License license = infoFields.getLicense();
        String version = infoFields.getVersion();

        Info concatenatedInfo = new Info();
        concatenatedInfo.setTitle(concatenatedTitle);
        concatenatedInfo.setDescription(description);
        concatenatedInfo.setSummary(summary);
        concatenatedInfo.setTermsOfService(termsOfService);
        concatenatedInfo.setContact(contact);
        concatenatedInfo.setLicense(license);
        concatenatedInfo.setVersion(version);
        openAPI.setInfo(concatenatedInfo);

        // Concatenate tags from all aggregated resources
        for (Tag tag : tags) {
            if (openAPI.getTags() == null) {
                openAPI.addTagsItem(tag);
            } else if (openAPI.getTags() != null && !openAPI.getTags().contains(tag)) {
                openAPI.addTagsItem(tag);
            }
        }

        Paths paths = new Paths();
        for (Map.Entry<String, Paths> entry : pathsMap.entrySet()) {
            for (Map.Entry<String, PathItem> pathItem : entry.getValue().entrySet()) {
                paths.addPathItem(pathItem.getKey(), pathItem.getValue());
            }
        }
        openAPI.setPaths(paths);

        // Set components and extensions for all aggregated resources
        openAPI.setComponents(components);
        openAPI.addExtension(OAS_EXTENSION_OH_FHIR_RESOURCE_TYPE, String.join(", ", resourceTypes));
        openAPI.addExtension(OAS_EXTENSION_OH_FHIR_PROFILE, String.join("\n", supportedProfiles));

        return openAPI;
    }
}
