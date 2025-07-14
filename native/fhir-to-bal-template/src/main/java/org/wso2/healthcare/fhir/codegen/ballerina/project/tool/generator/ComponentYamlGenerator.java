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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.AggregatedService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generator for component.yaml file for Choreo components.
 */
public class ComponentYamlGenerator extends AbstractFHIRTemplateGenerator {

    private static final Log LOG = LogFactory.getLog(ComponentYamlGenerator.class);

    public ComponentYamlGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = generatorProperties.get("projectAPIPath") + File.separator + ".choreo" + File.separator;
        File fileDir = new File(directoryPath);
        if (!fileDir.exists()) {
            if (!fileDir.mkdirs()) {
                LOG.error("Failed to create directories: " + fileDir);
                return;
            }
        }
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                        File.separator +"componentYaml.vm", createTemplateContext(generatorProperties), directoryPath,
                "component.yaml");
    }

    private TemplateContext createTemplateContext(Map<String, Object> generatorProperties) {
        TemplateContext templateContext = this.getNewTemplateContext();
        
        // Check if this is an aggregated service case
        if (generatorProperties.containsKey("aggregatedService")) {
            // Handle aggregated service with multiple endpoints
            AggregatedService aggregatedService = (AggregatedService) generatorProperties.get("aggregatedService");
            List<Map<String, String>> endpoints = new ArrayList<>();
            
            int portCounter = 9090;
            for (BallerinaService service : aggregatedService.getServices().values()) {
                Map<String, String> endpoint = createEndpointForService(service, generatorProperties, portCounter);
                endpoints.add(endpoint);
                portCounter++;
            }
            
            templateContext.setProperty("endpoints", endpoints);
        } else {
            // Handle single service case (existing logic)
            List<Map<String, String>> endpoints = new ArrayList<>();
            Map<String, String> endpoint = createEndpointForSingleService(generatorProperties);
            endpoints.add(endpoint);
            templateContext.setProperty("endpoints", endpoints);
        }
        
        return templateContext;
    }
    
    private Map<String, String> createEndpointForService(BallerinaService service, Map<String, Object> generatorProperties, int port) {
        Map<String, String> endpoint = new java.util.HashMap<>();
        
        if (generatorProperties.containsKey("dependentPackageImportIdentifier")) {
            String apiName = generatorProperties.get("dependentPackageImportIdentifier").toString().toLowerCase() +
                    "-" +
                    service.getName().toLowerCase() +
                    "-api";
            String displayName = generatorProperties.get("dependentPackageImportIdentifier").toString() + " " +
                    service.getName() + " API";
            endpoint.put("api_name", apiName);
            endpoint.put("api_display_name", displayName);
        } else {
            endpoint.put("api_name", service.getName().toLowerCase() + "-api");
            endpoint.put("api_display_name", service.getName() + " API");
        }
        endpoint.put("api_base_path", "/fhir/" + service.getFhirVersion() + "/" +service.getName());
        endpoint.put("api_oas_file", "oas/" + service.getName() + ".yaml");
        endpoint.put("api_port", String.valueOf(port));
        
        return endpoint;
    }
    
    private Map<String, String> createEndpointForSingleService(Map<String, Object> generatorProperties) {
        Map<String, String> endpoint = new java.util.HashMap<>();
        
        if (generatorProperties.containsKey("dependentPackageImportIdentifier")) {
            String apiName = generatorProperties.get("dependentPackageImportIdentifier").toString().toLowerCase() +
                    "-" +
                    generatorProperties.get("resourceType").toString().toLowerCase() +
                    "-api";
            String displayName = generatorProperties.get("dependentPackageImportIdentifier").toString() + " " +
                    generatorProperties.get("resourceType").toString() + " API";
            endpoint.put("api_name", apiName);
            endpoint.put("api_display_name", displayName);
        } else {
            endpoint.put("api_name", generatorProperties.get("resourceType").toString().toLowerCase() + "-api");
            endpoint.put("api_display_name", generatorProperties.get("resourceType").toString() + " API");
        }
        endpoint.put("api_base_path", "/" + generatorProperties.get("resourceType").toString());
        endpoint.put("api_oas_file", "oas/" + generatorProperties.get("resourceType").toString() + ".yaml");
        endpoint.put("api_port", "9090");
        
        return endpoint;
    }
}
