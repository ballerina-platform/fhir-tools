/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.healthcare.fhir.ballerina.prebuiltservicegen.tool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.AbstractToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTool;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This tool is used to generate a prebuilt service for a FHIR server.
 */
public class ServiceGenTool extends AbstractFHIRTool {

    private static final Log LOG = LogFactory.getLog(ServiceGenTool.class);
    private ServiceGenToolConfig prebuiltFhirServiceGenToolConfig;

    @Override
    public void initialize(ToolConfig toolConfig) throws CodeGenException {
        this.prebuiltFhirServiceGenToolConfig = (ServiceGenToolConfig) toolConfig;
    }

    @Override
    public TemplateGenerator execute(ToolContext toolContext) throws CodeGenException {
        Properties toolProperties = ((AbstractToolContext) toolContext).getCustomToolProperties();
        Map<String, Object> toolPropertiesMap = (Map<String, Object>) toolProperties.get("fhirServiceGenProperties");
        if (toolPropertiesMap != null) {
            Map<String, BallerinaService> serviceMap = (Map<String, BallerinaService>) toolPropertiesMap.get("serviceMap");
            int servicePort = 9090;
            for (Map.Entry<String, BallerinaService> entry : serviceMap.entrySet()) {
                String resourceType = entry.getKey();
                BallerinaService service = entry.getValue();
                Map<String, Object> projectProperties = new HashMap<>();
                projectProperties.put("service", service);
                projectProperties.put("resourceType", resourceType);
                projectProperties.put("config", toolPropertiesMap.get("config"));
                if (!service.getImportsList().contains(BallerinaProjectConstants.INTERNATIONAL_PACKAGE_IMPORT_SUFFIX)) {
                    ((Map<String, String>) toolPropertiesMap.get("dependenciesMap")).remove("dependentPackage");
                    service.getImportsList().remove(BallerinaProjectConstants.INTERNATIONAL_PACKAGE_IMPORT_SUFFIX);
                }
                projectProperties.put("dependencies", toolPropertiesMap.get("dependenciesMap"));
                Map<String, String> dependenciesMap = (Map<String, String>) toolPropertiesMap.get("dependenciesMap");
                String dependentPackage = dependenciesMap.get("dependentPackage");

                String basePackage = dependenciesMap.get("basePackage");
                String servicePackage = dependenciesMap.get("servicePackage");
                String igPackage = dependenciesMap.get("igPackage");
                projectProperties.put("basePackageImportIdentifier", basePackage.substring(
                        basePackage.lastIndexOf(".") + 1));
                projectProperties.put("servicePackageImportIdentifier", servicePackage.substring(
                        servicePackage.lastIndexOf(".") + 1));
                projectProperties.put("igPackageImportIdentifier", igPackage.substring(
                        igPackage.lastIndexOf(".") + 1));
                if (dependentPackage != null) {
                    projectProperties.put("dependentPackageImportIdentifier", dependentPackage.substring(
                            dependentPackage.lastIndexOf(".") + 1));
                }
                projectProperties.put("serviceFileName", service.getName().toLowerCase() + "_service.bal");
                projectProperties.put("apiConfigFileName", service.getName().toLowerCase() + "_api_config.bal");
                projectProperties.put("projectAPIPath", prebuiltFhirServiceGenToolConfig.getTargetDir() + File.separator
                        + prebuiltFhirServiceGenToolConfig.getProjectName());
                projectProperties.put("apiConfName", service.getName().toLowerCase().concat("ApiConfig"));
                projectProperties.put("servicePort", servicePort);
                projectProperties.put("serverName", prebuiltFhirServiceGenToolConfig.getFhirServerName());
                projectProperties.put("authMethod", prebuiltFhirServiceGenToolConfig.getAuthMethod());
                projectProperties.put("disablePackageMd", true);
                projectProperties.put("templateName", prebuiltFhirServiceGenToolConfig.getProjectName());
                if (service.getProfileList().size() > 0) {
                    projectProperties.put("profileList", service.getProfileList());
                }
                Map<String, String> interactionMethods = new HashMap<>();
                projectProperties.put("interactionMethods", interactionMethods);
                try {
                    ServiceInteractionMethodGenerator serviceInteractionMethodContentGenerator =
                            new ServiceInteractionMethodGenerator(prebuiltFhirServiceGenToolConfig.getTargetDir());
                    for (String interactionType : ToolConstants.SUPPORTED_INTERACTIONS) {
                        projectProperties.put("interactionType", interactionType);
                        serviceInteractionMethodContentGenerator.generate(toolContext, projectProperties);
                        String interactionMethodContent = serviceInteractionMethodContentGenerator.getInteractionMethodContent(
                                interactionType);
                        service.getFhirInteractionMethodsContent().setInteractionContentByType(
                                interactionType, interactionMethodContent);
                    }

                    ServiceGenerator serviceGenerator = new ServiceGenerator(
                            prebuiltFhirServiceGenToolConfig.getTargetDir());
                    serviceGenerator.generate(toolContext, projectProperties);
                } catch (CodeGenException e) {
                    LOG.error("Error occurred while generation FHIR prebuilt service.", e);
                }
                servicePort++;
            }
        }
        return null;
    }
}
