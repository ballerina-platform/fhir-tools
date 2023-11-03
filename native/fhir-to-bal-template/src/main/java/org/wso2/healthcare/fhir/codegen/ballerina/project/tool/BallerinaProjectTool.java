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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool;

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTool;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRResourceDef;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRSearchParamDef;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.IncludedIGConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator.BallerinaProjectGenerator;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.FHIRProfile;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.SearchParam;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class for Ballerina Project Generator tool.
 */
public class BallerinaProjectTool extends AbstractFHIRTool {
    private final Map<String, FHIRImplementationGuide> igMap = new HashMap<>();
    private final Map<String, BallerinaService> serviceMap = new HashMap<>();
    private final Map<String, String> dependenciesMap = new HashMap<>();
    private BallerinaProjectToolConfig ballerinaProjectToolConfig;

    @Override
    public void initialize(ToolConfig toolConfig) {
        this.ballerinaProjectToolConfig = (BallerinaProjectToolConfig) toolConfig;
    }

    @Override
    public TemplateGenerator execute(ToolContext toolContext) throws CodeGenException {
        if (ballerinaProjectToolConfig.isEnabled()) {
            populateIGs(toolContext);
            populateDependenciesMap();
            populateBalService();

            if (serviceMap.isEmpty()) {
                throw new CodeGenException("No services found to generate");
            }

            String targetRoot = ballerinaProjectToolConfig.getTargetDir();
            String targetDirectory = targetRoot + File.separator;
            BallerinaProjectGenerator balProjectGenerator = new BallerinaProjectGenerator(targetDirectory);
            Map<String, Object> generatorProperties = new HashMap<>();
            generatorProperties.put("config", ballerinaProjectToolConfig);
            generatorProperties.put("serviceMap", serviceMap);
            generatorProperties.put("dependenciesMap", dependenciesMap);
            balProjectGenerator.setGeneratorProperties(generatorProperties);
            return balProjectGenerator;
        }
        return null;
    }

    /**
     * Extract full IG based on the config.
     *
     * @param toolContext Tool context
     */
    private void populateIGs(ToolContext toolContext) {

        for (Map.Entry<String, IncludedIGConfig> entry : ballerinaProjectToolConfig.getIncludedIGConfigs().entrySet()) {
            String igName = entry.getKey();
            FHIRImplementationGuide ig = ((FHIRSpecificationData) toolContext.getSpecificationData()).
                    getFhirImplementationGuides().get(igName);
            String packageName = ballerinaProjectToolConfig.getMetadataConfig().getNamePrefix();
            if (entry.getValue().isEnable() && ig != null) {
                String igPackage = ballerinaProjectToolConfig.getMetadataConfig().getOrg() + "/" +
                        ballerinaProjectToolConfig.getMetadataConfig().getNamePrefix();
                igMap.put(packageName, ig);

                if (!packageName.equals(igName)) {
                    //Update key in the ig config global map
                    ballerinaProjectToolConfig.getIncludedIGConfigs().remove(igName);
                    IncludedIGConfig updatedIGConfig = entry.getValue();
                    updatedIGConfig.setImportStatement(igPackage);
                    ballerinaProjectToolConfig.getIncludedIGConfigs().put(packageName, updatedIGConfig);

                    ((FHIRSpecificationData) toolContext.getSpecificationData()).getFhirImplementationGuides().remove(igName);
                    ((FHIRSpecificationData) toolContext.getSpecificationData()).getFhirImplementationGuides().put(packageName, ig);
                }
                dependenciesMap.put("igPackage", igPackage);
            }
        }
    }

    /**
     * Populate Ballerina Service model according to configured IGs and Profiles.
     */
    private void populateBalService() {
        for (Map.Entry<String, FHIRImplementationGuide> entry : igMap.entrySet()) {
            String igName = entry.getKey();
            for (Map.Entry<String, FHIRResourceDef> definitionEntry : entry.getValue().getResources().entrySet()) {
                if (definitionEntry.getValue().getDefinition().getKind().toCode().equalsIgnoreCase("RESOURCE")) {
                    validateAndAddFHIRResource(definitionEntry.getValue().getDefinition(), igName);
                }
            }
            //adding Search parameters
            for (Map.Entry<String, FHIRSearchParamDef> parameter : entry.getValue().getSearchParameters().entrySet()) {
                List<CodeType> baseResources = parameter.getValue().getSearchParameter().getBase();
                for (CodeType baseType : baseResources) {
                    String apiName = baseType.getCode();
                    if (!serviceMap.containsKey(apiName)) {
                        continue;
                    }
                    SearchParam param = getSearchParam(parameter, apiName);
                    serviceMap.get(apiName).addSearchParam(param);
                }
            }
        }
    }

    private SearchParam getSearchParam(Map.Entry<String, FHIRSearchParamDef> parameter, String apiName) {
        SearchParam param = new SearchParam(parameter.getValue().getSearchParameter().getName(),
                parameter.getValue().getSearchParameter().getCode());
        param.setSearchParamDef(parameter.getValue().getSearchParameter());
        param.setDescription(parameter.getValue().getSearchParameter().getDescription());
        param.setDocumentation(parameter.getValue().getSearchParameter().getUrl());
        param.setTargetResource(apiName);
        if (ballerinaProjectToolConfig.getSearchParamConfigs().contains(param.getSearchParamDef().getCode())) {
            param.setBuiltIn(true);
        }
        return param;
    }

    /**
     * Validate Ballerina service based on include-exclude configs.
     *
     * @param structureDefinition FHIR StructureDefinition
     * @param igName              IG name
     */
    public void validateAndAddFHIRResource(StructureDefinition structureDefinition, String igName) {

        String resourceType = structureDefinition.getType();
        String profile = structureDefinition.getName();
        String url = structureDefinition.getUrl();

        if (ballerinaProjectToolConfig.getIncludedIGConfigs().get(igName).getIncludedProfiles().isEmpty()) {
            //add all resources of the IG except ones listed in excluded list
            if (!ballerinaProjectToolConfig.getIncludedIGConfigs().get(igName).getExcludedProfiles().contains(url)) {
                addResourceProfile(structureDefinition, resourceType, profile, url, igName);
            }
        } else {
            //add resources listed in included list. Neglect excluded list
            if (ballerinaProjectToolConfig.getIncludedIGConfigs().get(igName).getIncludedProfiles().contains(url)) {
                addResourceProfile(structureDefinition, resourceType, profile, url, igName);
            }
        }
    }

    /**
     * Adding Ballerina service model to a common map.
     *
     * @param structureDefinition FHIR StructureDefinition
     * @param resourceType        FHIR resource type
     * @param profile             FHIR profile
     * @param url                 FHIR profile url
     */
    private void addResourceProfile(StructureDefinition structureDefinition, String resourceType, String profile,
                                    String url, String igName) {

        if (serviceMap.containsKey(resourceType)) {
            if (structureDefinition.getAbstract()) {
                //profiled resource added before
                FHIRProfile fhirProfile = new FHIRProfile(structureDefinition, url, igName, resourceType);
                fhirProfile.setFhirVersion(ballerinaProjectToolConfig.getFhirVersion());
                fhirProfile.setPackagePrefix(ballerinaProjectToolConfig);
                fhirProfile.setAbstract();
                fhirProfile.addImport(ballerinaProjectToolConfig.getIncludedIGConfigs().get(igName).getImportStatement());
                serviceMap.get(resourceType).addFhirProfile(fhirProfile);
                serviceMap.get(resourceType).addProfile(url);
            } else {
                //check for profiles
                if (!serviceMap.get(resourceType).getProfiles().contains(profile)) {
                    FHIRProfile fhirProfile = new FHIRProfile(structureDefinition, url, igName, resourceType);
                    fhirProfile.addImport(ballerinaProjectToolConfig.getIncludedIGConfigs().get(igName).getImportStatement());
                    fhirProfile.setFhirVersion(ballerinaProjectToolConfig.getFhirVersion());
                    fhirProfile.setPackagePrefix(ballerinaProjectToolConfig);
                    serviceMap.get(resourceType).addFhirProfile(fhirProfile);
                    serviceMap.get(resourceType).addProfile(url);
                }
            }
        } else {
            BallerinaService ballerinaService = new BallerinaService(resourceType, ballerinaProjectToolConfig.getFhirVersion());
            FHIRProfile fhirProfile = new FHIRProfile(structureDefinition, url, igName, resourceType);
            fhirProfile.addImport(ballerinaProjectToolConfig.getIncludedIGConfigs().get(igName).getImportStatement());
            fhirProfile.setFhirVersion(ballerinaProjectToolConfig.getFhirVersion());
            fhirProfile.setPackagePrefix(ballerinaProjectToolConfig);
            ballerinaService.addFhirProfile(fhirProfile);
            ballerinaService.addProfile(url);
            ballerinaService.addIg(igName);
            serviceMap.put(structureDefinition.getType(), ballerinaService);
        }
    }

    private void populateDependenciesMap() {
        String fhirVersion = ballerinaProjectToolConfig.getFhirVersion();

        String fhirBaseImportStatement = BallerinaProjectConstants.BASE_PACKAGE_IMPORT_SUFFIX + fhirVersion;
        String fhirServiceImportStatement = BallerinaProjectConstants.SERVICE_PACKAGE_IMPORT_SUFFIX + fhirVersion;
        String fhirInternationalImportStatement =
                BallerinaProjectConstants.INTERNATIONAL_PACKAGE_IMPORT_SUFFIX;

        if (ballerinaProjectToolConfig.getBasePackage() != null && !ballerinaProjectToolConfig.getBasePackage().isEmpty()) {
            fhirBaseImportStatement = ballerinaProjectToolConfig.getBasePackage();
        }

        if (ballerinaProjectToolConfig.getServicePackage() != null && !ballerinaProjectToolConfig.getServicePackage().isEmpty()) {
            fhirServiceImportStatement = ballerinaProjectToolConfig.getServicePackage();
        }

        if (ballerinaProjectToolConfig.getDependentPackage() != null &&
                !ballerinaProjectToolConfig.getDependentPackage().isEmpty()) {
            fhirInternationalImportStatement = ballerinaProjectToolConfig.getDependentPackage();
        }
        dependenciesMap.put("basePackage", fhirBaseImportStatement.toLowerCase());
        dependenciesMap.put("servicePackage", fhirServiceImportStatement.toLowerCase());
        dependenciesMap.put("dependentPackage", fhirInternationalImportStatement.toLowerCase());
    }
}
