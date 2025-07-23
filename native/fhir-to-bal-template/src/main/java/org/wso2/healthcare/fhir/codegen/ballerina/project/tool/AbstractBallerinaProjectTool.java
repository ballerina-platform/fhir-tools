/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTool;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRSearchParamDef;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.IncludedIGConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator.BallerinaProjectGenerator;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.AggregatedService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.SearchParam;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class AbstractBallerinaProjectTool extends AbstractFHIRTool {
    private final Map<String, FHIRImplementationGuide> igMap = new HashMap<>();
    private final Map<String, BallerinaService> serviceMap = new HashMap<>();
    private final Map<String, AggregatedService> aggregatedServiceMap = new HashMap<>();
    private final Map<String, String> dependenciesMap = new HashMap<>();

    private final List<String> EXCLUDED_FHIR_APIS = new ArrayList<>(Arrays.asList("Bundle",
            "CodeSystem", "DomainResource", "OperationOutcome", "Resource", "ValueSet"));
    private BallerinaProjectToolConfig ballerinaProjectToolConfig;

    @Override
    public void initialize(ToolConfig toolConfig) throws CodeGenException {
        this.ballerinaProjectToolConfig = (BallerinaProjectToolConfig) toolConfig;
    }

    protected BallerinaProjectToolConfig getBallerinaProjectToolConfig() {
        return ballerinaProjectToolConfig;
    }

    protected Map<String, FHIRImplementationGuide> getIgMap() {
        return igMap;
    }

    protected Map<String, String> getDependenciesMap() {
        return dependenciesMap;
    }

    protected Map<String, BallerinaService> getServiceMap() {
        return serviceMap;
    }

    protected List<String> getExcludedFHIRApis() {
        return EXCLUDED_FHIR_APIS;
    }

    @Override
    public TemplateGenerator execute(ToolContext toolContext) throws CodeGenException {
        if (getBallerinaProjectToolConfig().isEnabled()) {
            populateIGs(toolContext);
            populateDependenciesMap();
            populateBalService();

            if (getServiceMap().isEmpty()) {
                throw new CodeGenException("No services found to generate");
            }

            // Handle aggregated API generation if enabled
            if (ballerinaProjectToolConfig.isEnableAggregatedApi()) {
                populateAggregatedServices();
            }

            String targetRoot = getBallerinaProjectToolConfig().getTargetDir();
            String targetDirectory = targetRoot + File.separator;
            BallerinaProjectGenerator balProjectGenerator = new BallerinaProjectGenerator(targetDirectory);
            Map<String, Object> generatorProperties = new HashMap<>();
            generatorProperties.put("config", getBallerinaProjectToolConfig());
            generatorProperties.put("serviceMap", getServiceMap());
            generatorProperties.put("aggregatedServiceMap", aggregatedServiceMap);
            generatorProperties.put("dependenciesMap", getDependenciesMap());
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
    protected void populateIGs(ToolContext toolContext) {

        for (Map.Entry<String, IncludedIGConfig> entry : getBallerinaProjectToolConfig().getIncludedIGConfigs().entrySet()) {
            String igName = entry.getKey();
            FHIRImplementationGuide ig = ((FHIRSpecificationData) toolContext.getSpecificationData()).
                    getFhirImplementationGuides().get(igName);
            String packageName = getBallerinaProjectToolConfig().getVersionConfig().getNamePrefix();
            if (entry.getValue().isEnable() && ig != null) {
                String igPackage = getBallerinaProjectToolConfig().getMetadataConfig().getOrg() + "/" +
                        getBallerinaProjectToolConfig().getVersionConfig().getNamePrefix();
                igMap.put(packageName, ig);

                if (!packageName.equals(igName)) {
                    //Update key in the ig config global map
                    getBallerinaProjectToolConfig().getIncludedIGConfigs().remove(igName);
                    IncludedIGConfig updatedIGConfig = entry.getValue();
                    updatedIGConfig.setImportStatement(igPackage);
                    getBallerinaProjectToolConfig().getIncludedIGConfigs().put(packageName, updatedIGConfig);

                    ((FHIRSpecificationData) toolContext.getSpecificationData()).getFhirImplementationGuides().remove(igName);
                    ((FHIRSpecificationData) toolContext.getSpecificationData()).getFhirImplementationGuides().put(packageName, ig);
                }
                dependenciesMap.put("igPackage", igPackage);
            }
        }
    }

    private void populateAggregatedServices() {
        List<String> apiGroups = ballerinaProjectToolConfig.getAggregatedApis();

        if (apiGroups.isEmpty()) {
            // If no specific groups are configured, create one aggregated service with all APIs
            AggregatedService aggregatedService = new AggregatedService("AggregatedFHIRService",
                    ballerinaProjectToolConfig.getFhirVersion());

            for (Map.Entry<String, BallerinaService> entry : serviceMap.entrySet()) {
                aggregatedService.addService(entry.getKey(), entry.getValue());
            }

            aggregatedServiceMap.put("aggregated", aggregatedService);
        } else {
            AggregatedService aggregatedService = new AggregatedService("AggregatedFHIRService",
                    ballerinaProjectToolConfig.getFhirVersion());
            // Create aggregated services based on configured groups
            for (String apiName : apiGroups) {
                if (serviceMap.containsKey(apiName)) {
                    aggregatedService.addService(apiName, serviceMap.get(apiName));
                }
                aggregatedServiceMap.put("aggregated", aggregatedService);
            }
        }
    }

    protected void populateDependenciesMap() {
        String fhirVersion = getBallerinaProjectToolConfig().getFhirVersion();

        String fhirBaseImportStatement = BallerinaProjectConstants.BASE_PACKAGE_IMPORT_SUFFIX + fhirVersion;
        String fhirServiceImportStatement = BallerinaProjectConstants.SERVICE_PACKAGE_IMPORT_SUFFIX + fhirVersion;
        String fhirInternationalImportStatement = BallerinaProjectConstants.INTERNATIONAL_PACKAGE_IMPORT_SUFFIX_R4;

        if (fhirVersion.equalsIgnoreCase("r5")) {
            fhirInternationalImportStatement = BallerinaProjectConstants.INTERNATIONAL_PACKAGE_IMPORT_SUFFIX_R5;
        }

        if (getBallerinaProjectToolConfig().getVersionConfig().getBasePackage() != null && !getBallerinaProjectToolConfig().getVersionConfig().getBasePackage().isEmpty()) {
            fhirBaseImportStatement = getBallerinaProjectToolConfig().getVersionConfig().getBasePackage();
        }

        if (getBallerinaProjectToolConfig().getVersionConfig().getServicePackage() != null && !getBallerinaProjectToolConfig().getVersionConfig().getServicePackage().isEmpty()) {
            fhirServiceImportStatement = getBallerinaProjectToolConfig().getVersionConfig().getServicePackage();
        }

        if (getBallerinaProjectToolConfig().getVersionConfig().getDependentPackage() != null &&
                !getBallerinaProjectToolConfig().getVersionConfig().getDependentPackage().isEmpty()) {
            fhirInternationalImportStatement = getBallerinaProjectToolConfig().getVersionConfig().getDependentPackage();
        }
        dependenciesMap.put("basePackage", fhirBaseImportStatement.toLowerCase());
        dependenciesMap.put("servicePackage", fhirServiceImportStatement.toLowerCase());
        dependenciesMap.put("dependentPackage", fhirInternationalImportStatement.toLowerCase());
    }

    protected abstract void populateBalService();

    protected abstract SearchParam getSearchParam(Map.Entry<String, FHIRSearchParamDef> parameter, String apiName);
}
