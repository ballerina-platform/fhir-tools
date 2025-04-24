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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r4.oas.model.R4APIDefinition;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Generator class for OAS definitions for the FHIR templates.
 */
public class OasGenerator extends AbstractFHIRTemplateGenerator {

    private static final Log LOG = LogFactory.getLog(OasGenerator.class);

    public OasGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
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
        String resourceType = (String) generatorProperties.get("resourceType");
        for (Map.Entry<String, FHIRImplementationGuide> entry :
                ((FHIRSpecificationData) toolContext.getSpecificationData()).getFhirImplementationGuides().entrySet()) {
            R4APIDefinition apiDefinition = entry.getValue().getApiDefinitions().get(resourceType);
            if (apiDefinition != null) {
                String oasDefYaml = Yaml.pretty(apiDefinition.getOpenAPI());

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(directoryPath + File.separator +
                        resourceType + BallerinaProjectConstants.YAML_FILE_EXTENSION))) {
                    writer.write(oasDefYaml);
                } catch (IOException e) {
                    LOG.error("Error occurred while writing OAS Def to file.", e);
                }
            }
        }
    }
}
