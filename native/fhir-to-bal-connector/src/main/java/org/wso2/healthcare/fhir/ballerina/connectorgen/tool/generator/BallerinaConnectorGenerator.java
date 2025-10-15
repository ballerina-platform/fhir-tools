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
        System.out.println("Fetched CapabilityStatement from: " + fhirServerUrl);
        if (capabilityStatement == null) {
            throw new CodeGenException("Failed to fetch CapabilityStatement from the FHIR server.");
        }

        Map<String, FHIRResource> resourceMap = getStringFHIRResourceMap(toolConfig);
        TemplateContext templateContext = this.buildContextFromCapability(capabilityStatement, resourceMap, toolConfig);

        // Step 1: Copy the ballerina-connector-tool directory
        Path targetDir = Paths.get(this.getTargetDir(), Constants.BALLERINA_CONNECTOR_TOOL);
        try {
            CommonUtils.copyResourceDir(getClass().getClassLoader().getResource(Constants.BALLERINA_CONNECTOR_TOOL), targetDir);
        }  catch (IOException e) {
            throw new CodeGenException("Failed to copy ballerina-connector-tool directory.", e);
        } catch (URISyntaxException e) {
            throw new CodeGenException("Cannot find ballerina-connector-tool directory.", e);
        }

        String filePath = targetDir.resolve("fhir_connector.bal").toString();
        System.out.println("Generating Ballerina FHIR connector at: " + filePath);
        this.getTemplateEngine().generateOutputAsFile("template/fhir_connector.vm", templateContext, "", filePath);

    }

    private static Map<String, FHIRResource> getStringFHIRResourceMap(BallerinaConnectorGenToolConfig toolConfig) {
        Map<String, FHIRResource> resourceMap = new HashMap<>();

        String orgName = toolConfig.getCentralConfig().getOrgName();
        String ballerinaCentralURL = toolConfig.getCentralConfig().getUrl();

        List<String> profilePackages = toolConfig.getCentralConfig().getProfilePackages();
        for (String profilePackage : profilePackages) {
            String[] parts = profilePackage.split(":");
            if (parts.length != 2) {
                System.err.println("Invalid profile package format: " + profilePackage);
                continue;
            }
            String packageName = parts[0];
            String packageVersion = parts[1];
            String readMeStr = HttpUtils.getReadMe(ballerinaCentralURL,
                    orgName,
                    packageName,
                    packageVersion);

            if (readMeStr == null || readMeStr.isEmpty()) {
                System.out.println("Empty README content for package: " + profilePackage);
                continue;
            }

            Map<String, FHIRResource> parsedResources = TextParserUtils.parseReadMeString(readMeStr, packageName, StringUtils.toCamelCase(packageName));
            resourceMap.putAll(parsedResources);
        }
        return resourceMap;
    }

    public TemplateContext buildContextFromCapability(CapabilityStatement capabilityStatement, Map<String, FHIRResource> resourceMap, BallerinaConnectorGenToolConfig toolConfig) {
       List<Map<String, Object>> resources = capabilityStatement.getRest().stream()
                .flatMap(rest -> rest.getResource().stream())
                .filter(resource -> !Constants.SKIP_LIST.contains(resource.getType()))
                .map(resource -> {
                    Map<String, Object> resMap = new HashMap<>();
                    resMap.put(Constants.TYPE, resource.getType());
                    resMap.put(Constants.INTERACTIONS, resource.getInteraction() == null ? List.of() :
                            resource.getInteraction().stream().map(Interaction::getCode).toList());
                    resMap.put(Constants.SEARCH_PARAMS, resource.getSearchParam() == null ? List.of() :
                            resource.getSearchParam().stream().map(param -> Map.of(Constants.RESOLVED_NAME, StringUtils.resolveSpecialCharacters(StringUtils.handleBallerinaKeyword(param.getName())),
                                    Constants.ORIGINAL_NAME, param.getName(),
                                    Constants.TYPE, param.getType(),
                                    Constants.DOCUMENTATION, param.getDocumentation() != null ? TextParserUtils.extractCommentFromText(param.getDocumentation(), resource.getType()) : ""
                            )).toList());
                    if (resMap.get(Constants.INTERACTIONS) != null && !((List<?>) resMap.get(Constants.INTERACTIONS)).isEmpty()) {
                        if (((List<?>) resMap.get(Constants.INTERACTIONS)).contains("create") || ((List<?>) resMap.get(Constants.INTERACTIONS)).contains("update") || ((List<?>) resMap.get(Constants.INTERACTIONS)).contains("patch")) {
                            resMap.put(Constants.SUPPORTED_PROFILES, resource.getSupportedProfile());
                            String typeString = buildProfiles(resource, resourceMap);
                            resMap.put(Constants.TYPE_STRING, typeString);
                            if (typeString.contains(Constants.RESOURCE_INTERNATIONAL)) {
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
                        String internationalPackage = toolConfig.getCentralConfig().getInternationalPackage().contains(":") ?
                                toolConfig.getCentralConfig().getInternationalPackage().split(":")[0] :
                                toolConfig.getCentralConfig().getInternationalPackage();
                        String importStr = toolConfig.getCentralConfig().getOrgName() + "/" + internationalPackage + " as resourceInternational";
                        if (!packageNames.contains(importStr)) {
                            packageNames.add(importStr);
                        }
                    }
                });

        templateContext.setProperty(Constants.PROFILE_PACKAGES, packageNames);
        return templateContext;
    }

    private String buildProfiles(Resource resource, Map<String, FHIRResource> resourceMap) {
        if (resource.getSupportedProfile() == null || resource.getSupportedProfile().isEmpty()) {
           return Constants.RESOURCE_INTERNATIONAL + resource.getType();
        }
        StringBuilder typeString = new StringBuilder();
        boolean internationalSet = false;
        for (int i = 0; i < resource.getSupportedProfile().size(); i++) {
            String profile = resource.getSupportedProfile().get(i);
            String profileName = resourceMap.get(profile) != null ? resourceMap.get(profile).getName() : null;
            if (profileName == null && !internationalSet) {
                if (i > 0) typeString.append("|");
                typeString.append(Constants.RESOURCE_INTERNATIONAL).append(resource.getType());
                internationalSet = true;
            } else if (profileName != null) {
                if (i > 0) typeString.append("|");
                typeString.append(resourceMap.get(profile).getResourcePackageAlias()).append(":").append(profileName);
            }
        }

        return typeString.toString();
    }
}
