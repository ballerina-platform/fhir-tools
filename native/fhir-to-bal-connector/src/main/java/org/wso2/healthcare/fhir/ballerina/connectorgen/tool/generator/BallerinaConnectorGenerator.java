package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.generator;


import org.wso2.healthcare.codegen.tool.framework.commons.core.AbstractTemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateEngine;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;

import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.Constants;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.config.BallerinaConnectorGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.model.CapabilityStatement;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.model.FHIRResource;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.util.CommonUtils;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.util.HttpUtils;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.util.StringUtils;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.util.TextParserUtils;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.model.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BallerinaConnectorGenerator extends AbstractTemplateGenerator {

    public BallerinaConnectorGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(TemplateEngine templateEngine, ToolContext toolContext, Map<String, Object> map) throws CodeGenException {
        this.setTemplateEngine(templateEngine);
        this.generate(toolContext, map);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> properties) throws CodeGenException {

        BallerinaConnectorGenToolConfig toolConfig = (BallerinaConnectorGenToolConfig) properties.get("toolConfig");
        String fhirServerUrl = toolConfig.getFhirServerUrl();
        if (fhirServerUrl == null || fhirServerUrl.isEmpty()) {
            throw new CodeGenException("FHIR server URL is not provided in the configuration.");
        }

        CapabilityStatement capabilityStatement = HttpUtils.getCapabilityStatement(fhirServerUrl);
        System.out.println("[INFO] Fetched CapabilityStatement from: " + fhirServerUrl);
        if (capabilityStatement == null) {
            throw new CodeGenException("Failed to fetch CapabilityStatement from the FHIR server.");
        }

        Map<String, FHIRResource> resourceMap = getStringFHIRResourceMap(toolConfig);
        TemplateContext templateContext = this.buildContextFromCapability(capabilityStatement, resourceMap, toolConfig);
        templateContext.setProperty("capabilityUrl", fhirServerUrl);

        // Step 1: Copy the ballerina-connector-tool directory
        Path targetDir = Paths.get(this.getTargetDir(), Constants.BALLERINA_CONNECTOR_TOOL);
        try {
            CommonUtils.copyResourceDir(getClass().getClassLoader().getResource(Constants.BALLERINA_CONNECTOR_TOOL), targetDir);
        } catch (IOException e) {
            throw new CodeGenException("Failed to copy ballerina-connector-tool directory.", e);
        } catch (URISyntaxException e) {
            throw new CodeGenException("Cannot find ballerina-connector-tool directory.", e);
        }

        String filePath = targetDir.resolve("fhir_connector.bal").toString();
        System.out.println("[INFO] Generating Ballerina FHIR connector at: " + filePath);

        // Step 2: Generate fhir_connector.bal
        this.getTemplateEngine().generateOutputAsFile("template/fhir_connector.vm", templateContext, "", filePath);

        // Step 3: Generate README.md
        this.getTemplateEngine().generateOutputAsFile("template/readMe.vm", templateContext, "", targetDir.resolve("README.md").toString());

    }

    /**
     * Fetch and parse FHIR resource profiles from Ballerina Central
     */
    private static Map<String, FHIRResource> getStringFHIRResourceMap(BallerinaConnectorGenToolConfig toolConfig) {
        Map<String, FHIRResource> resourceMap = new HashMap<>();

        String orgName = toolConfig.getCentralConfig().getOrgName();
        String ballerinaCentralURL = toolConfig.getCentralConfig().getUrl();

        List<String> profilePackages = toolConfig.getCentralConfig().getProfilePackages();
        for (String profilePackage : profilePackages) {

            String packageVersion = HttpUtils.getLatestVersionOfPackage(ballerinaCentralURL,
                    orgName,
                    profilePackage);

            if (packageVersion == null || packageVersion.isEmpty()) {
                System.err.println("[WARNING] Could not find package: " + profilePackage + " in Ballerina Central. Skipping...");
                continue;
            }

            String readMeStr = HttpUtils.getReadMe(ballerinaCentralURL,
                    orgName,
                    profilePackage,
                    packageVersion);

            if (readMeStr == null || readMeStr.isEmpty()) {
                System.err.println("[WARNING] Empty README content for package: " + profilePackage + ". Skipping...");
                continue;
            }

            String[] parts = profilePackage.split("\\.");
            Map<String, FHIRResource> parsedResources = TextParserUtils.parseReadMeString(readMeStr, profilePackage, StringUtils.toCamelCase(parts));
            resourceMap.putAll(parsedResources);
        }
        return resourceMap;
    }

    /**
     * Build template context from CapabilityStatement
     */
    public TemplateContext buildContextFromCapability(CapabilityStatement capabilityStatement, Map<String, FHIRResource> resourceMap, BallerinaConnectorGenToolConfig toolConfig) {
        List<Map<String, Object>> resources = capabilityStatement.getRest().stream()
                .flatMap(rest -> rest.getResource().stream())
                .filter(resource -> !Constants.SKIP_LIST.contains(resource.getType()))
                .map(resource -> {
                    Map<String, Object> resMap = new HashMap<>();
                    resMap.put(Constants.TYPE, resource.getType());
                    resMap.put(Constants.INTERACTIONS, resource.getInteraction() == null ? List.of() :
                            resource.getInteraction().stream().map(Interaction::getCode).toList());
                    resMap.put(Constants.OPERATIONS, resource.getOperation() == null ? List.of() :
                            buildOperations(resource.getOperation()));
                    resMap.put(Constants.SEARCH_PARAMS, resource.getSearchParam() == null ? List.of() :
                            resource.getSearchParam().stream().map(param -> Map.of(Constants.RESOLVED_NAME, StringUtils.resolveSpecialCharacters(StringUtils.handleBallerinaKeyword(param.getName())),
                                    Constants.ORIGINAL_NAME, param.getName(),
                                    Constants.TYPE, param.getType(),
                                    Constants.DOCUMENTATION, param.getDocumentation() != null ? TextParserUtils.extractCommentFromText(param.getDocumentation(), resource.getType()) : ""
                            )).toList());
                    if (resMap.get(Constants.INTERACTIONS) != null && !((List<?>) resMap.get(Constants.INTERACTIONS)).isEmpty()) {
                        if (((List<?>) resMap.get(Constants.INTERACTIONS)).contains(Constants.INTERACTIONS_CREATE) || ((List<?>) resMap.get(Constants.INTERACTIONS)).contains(Constants.INTERACTIONS_UPDATE) || ((List<?>) resMap.get(Constants.INTERACTIONS)).contains(Constants.INTERACTIONS_PATCH)) {
                            resMap.put(Constants.SUPPORTED_PROFILES, resource.getSupportedProfile());
                            String typeString = buildProfiles(resource, resourceMap);
                            resMap.put(Constants.TYPE_STRING, typeString);
                            if (typeString.contains(Constants.RESOURCE_INTERNATIONAL + ":")) {
                                resMap.put(Constants.INTERNATIONAL_PACKAGE, true);
                            }
                        }
                    }
                    return resMap;
                })
                .toList();

        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty(Constants.RESOURCES, resources);

        List<String> packageNames = new ArrayList<>();
        resources.stream()
                .filter(res -> res.get(Constants.SUPPORTED_PROFILES) != null)
                .forEach(res -> {
                    List<String> profiles = (List<String>) res.get(Constants.SUPPORTED_PROFILES);
                    profiles.forEach(profile -> {
                        if (resourceMap.containsKey(profile)) {
                            String packageName = toolConfig.getCentralConfig().getOrgName() + "/" + resourceMap.get(profile).getResourcePackage();
                            if (!packageNames.contains(packageName)) {
                                String importStr = packageName + " as " + resourceMap.get(profile).getResourcePackageAlias();
                                if (packageNames.contains(importStr)) {
                                    return;
                                }
                                packageNames.add(importStr);
                            }
                        }
                    });

                    if (res.containsKey(Constants.INTERNATIONAL_PACKAGE) && (Boolean) res.get(Constants.INTERNATIONAL_PACKAGE)) {
                        String importStr = toolConfig.getCentralConfig().getOrgName() + "/" + Constants.BASE_PACKAGE + " as " + Constants.RESOURCE_INTERNATIONAL;
                        if (!packageNames.contains(importStr)) {
                            packageNames.add(importStr);
                        }
                    }
                });

        templateContext.setProperty(Constants.PROFILE_PACKAGES, packageNames);
        return templateContext;
    }

    /**
     * Build type string from supported profiles
     */
    private String buildProfiles(Resource resource, Map<String, FHIRResource> resourceMap) {
        if (resource.getSupportedProfile() == null || resource.getSupportedProfile().isEmpty()) {
            return Constants.RESOURCE_INTERNATIONAL + ":" + resource.getType();
        }
        StringBuilder typeString = new StringBuilder();
        boolean internationalSet = false;
        for (int i = 0; i < resource.getSupportedProfile().size(); i++) {
            String profile = resource.getSupportedProfile().get(i);
            String profileName = resourceMap.get(profile) != null ? resourceMap.get(profile).getName() : null;
            if (profileName == null) {
                System.err.println("[WARNING] Profile " + profile + " not found for resource " + resource.getType()
                        + ". Defaulting to base package.");
                if (!internationalSet) {
                    if (i > 0) typeString.append("|");
                    typeString.append(Constants.RESOURCE_INTERNATIONAL).append(":").append(resource.getType());
                    internationalSet = true;
                }
            } else {
                if (i > 0) typeString.append("|");
                typeString.append(resourceMap.get(profile).getResourcePackageAlias()).append(":").append(profileName);
            }
        }

        return typeString.toString();
    }

    /**
     * Build connector operations from resource operations
     */
    private List<ConnectorOperation> buildOperations(List<Operation> operations) {
        // Implementation for building operations
        List<ConnectorOperation> connectorOperations = new ArrayList<>();
        for (Operation operation : operations) {
            ConnectorOperation connOp = new ConnectorOperation();
            connOp.setName(operation.getName());
            connOp.setDefinition(operation.getDefinition());
            connOp.setDocumentation(operation.getDocumentation());

            String[] parts = operation.getName().split("-+");
            String connectorOperationName = StringUtils.toCamelCase(parts);
            connOp.setFunctionName(connectorOperationName);

            connOp.setHttpMethod("POST"); // FHIR operations are typically invoked using POST

            // Compare with list of known operations that use GET
            if (Constants.KNOWN_GET_OPERATIONS.contains(operation.getName())) {
                connOp.setHttpMethod("GET");
            }

            connectorOperations.add(connOp);
        }
        return connectorOperations;
    }

}
