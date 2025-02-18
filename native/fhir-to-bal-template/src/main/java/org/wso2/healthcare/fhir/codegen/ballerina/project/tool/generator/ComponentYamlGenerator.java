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

import java.io.File;
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
        if (generatorProperties.containsKey("dependentPackageImportIdentifier")) {
            String apiName = generatorProperties.get("dependentPackageImportIdentifier").toString().toLowerCase() +
                    "-" +
                    generatorProperties.get("resourceType").toString().toLowerCase() +
                    "-api";
            String displayName = generatorProperties.get("dependentPackageImportIdentifier").toString() + " " +
                    generatorProperties.get("resourceType").toString() + " API";
            templateContext.setProperty("api_name", apiName);
            templateContext.setProperty("api_display_name", displayName);
        } else {
            templateContext.setProperty("api_name", generatorProperties.get("resourceType").toString().toLowerCase() + "-api");
            templateContext.setProperty("api_display_name", generatorProperties.get("resourceType").toString() + " API");
        }
        templateContext.setProperty("api_base_path", "/" + generatorProperties.get("resourceType").toString());
        templateContext.setProperty("api_oas_file", "oas/" + generatorProperties.get("resourceType").toString() +
                BallerinaProjectConstants.YAML_FILE_EXTENSION);
        return templateContext;
    }
}
