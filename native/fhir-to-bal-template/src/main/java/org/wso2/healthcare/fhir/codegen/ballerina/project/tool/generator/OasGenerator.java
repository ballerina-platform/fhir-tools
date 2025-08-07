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

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.oas.model.APIDefinition;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.AggregatedService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.OpenApiDef;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Generator class for OAS definitions for the FHIR templates.
 */
public class OasGenerator extends AbstractFHIRTemplateGenerator {

    private static final Log LOG = LogFactory.getLog(OasGenerator.class);

    // Variables to store API definitions for all resources specified in aggregated mode.
    private static Map<String, APIDefinition> aggregatedResourceApiDefinitions;

    public OasGenerator(String targetDir) throws CodeGenException {
        super(targetDir);

        // Initialize variables for aggregated mode.
        aggregatedResourceApiDefinitions = new HashMap<>();
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = generatorProperties.get("projectAPIPath") + File.separator
                + BallerinaProjectConstants.OAS_DEF_DIR_NAME;
        File fileDir = new File(directoryPath);
        if (!fileDir.exists()) {
            if (!fileDir.mkdirs()) {
                LOG.error("Failed to create directories: " + fileDir);
                return;
            }
        }

        AggregatedService aggregatedService = (AggregatedService) generatorProperties.get("aggregatedService");

        if (aggregatedService == null) {
            String resourceType = (String) generatorProperties.get("resourceType");

            for (Map.Entry<String, FHIRImplementationGuide> entry :
                    ((FHIRSpecificationData) toolContext.getSpecificationData()).getFhirImplementationGuides().entrySet()) {

                APIDefinition apiDefinition = entry.getValue().getApiDefinitions().get(resourceType);
                if (apiDefinition != null) {
                    String oasDefYaml = Yaml.pretty(apiDefinition.getOpenAPI());
                    writeToYamlFile(directoryPath, oasDefYaml, resourceType);
                }
            }
        } else {
            Set<String> resourceTypes = (Set<String>) generatorProperties.get("resourceTypes");

            for (String resourceType : resourceTypes) {
                for (FHIRImplementationGuide guide : ((FHIRSpecificationData) toolContext.getSpecificationData()).getFhirImplementationGuides().values()) {
                    APIDefinition apiDefinition = guide.getApiDefinitions().get(resourceType);
                    if (apiDefinition != null) {
                        aggregatedResourceApiDefinitions.put(resourceType, apiDefinition);
                        break;
                    }
                }
            }

            try {
                OpenApiDef.getInstance().retrieveFieldValues(aggregatedResourceApiDefinitions);
                OpenAPI newOpenApiDef = OpenApiDef.createNewOpenAPIDef();
                writeToYamlFile(directoryPath, Yaml.pretty(newOpenApiDef), "oas-definition");
            } catch (NullPointerException e) {
                throw new CodeGenException("Error occurred while generating open-api-def in aggregated mode: " + e.getMessage(),e);
            }
        }
    }

    /**
     * Writes the OAS definition to a YAML file.
     *
     * @param directoryPath The directory where the file will be written.
     * @param oasDefYaml    The OAS definition in YAML format.
     * @param filename      The name of the file to write.
     */
    private static void writeToYamlFile(String directoryPath, String oasDefYaml, String filename) throws CodeGenException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directoryPath + File.separator +
                filename + BallerinaProjectConstants.YAML_FILE_EXTENSION))) {
            writer.write(oasDefYaml);
        } catch (IOException e) {
            throw new CodeGenException("Error occurred while writing to YAML file: " + e.getMessage(), e);
        }
    }
}
