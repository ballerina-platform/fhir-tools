package org.wso2.healthcare.fhir.ballerina.connectorgen.tool;

import java.util.List;

public class Constants {
    public static final String TYPE = "type";
    public static final String INTERACTIONS = "interactions";
    public static final String OPERATIONS = "operations";
    public static final String SEARCH_PARAMS = "searchParams";
    public static final String ORIGINAL_NAME = "originalName";
    public static final String RESOLVED_NAME = "resolvedName";
    public static final String DOCUMENTATION = "documentation";
    public static final String SUPPORTED_PROFILES = "supportedProfiles";
    public static final String TYPE_STRING = "typeString";
    public static final String PROFILE_PACKAGES = "profilePackages";
    public static final String INTERNATIONAL_PACKAGE = "internationalPackage";
    public static final String RESOURCES = "resources";
    public static final String RESOURCE_INTERNATIONAL = "resourceInternational";
    public static final String BALLERINA_CONNECTOR_TOOL = "ballerina-connector-tool";

    public static final String BASE_PACKAGE = "health.fhir.r4.international401";

    public static final List<String> SKIP_LIST = List.of(
            "Bundle",
            "CodeSystem",
            "OperationOutcome",
            "ValueSet"
    );

    public static final List<String> KNOWN_GET_OPERATIONS = List.of(
            "find",
            "everything",
            "meta",
            "versions",
            "data-requirements"
    );

    public static final String INTERACTIONS_CREATE = "create";
    public static final String INTERACTIONS_UPDATE = "update";
    public static final String INTERACTIONS_PATCH = "patch";

}
