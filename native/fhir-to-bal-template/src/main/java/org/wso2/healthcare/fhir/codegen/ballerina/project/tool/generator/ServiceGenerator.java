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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator;

import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Generator for Ballerina service files.
 */
public class ServiceGenerator extends AbstractFHIRTemplateGenerator {

    public ServiceGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = generatorProperties.get("projectAPIPath") + File.separator;
        String serviceFileName = "service.bal";
        if (generatorProperties.containsKey("serviceFileName")) {
            serviceFileName = (String) generatorProperties.get("serviceFileName");
        }
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                File.separator + "balService.vm", createTemplateContextForBalService(
                generatorProperties), directoryPath, serviceFileName);
    }

    private BallerinaService initializeServiceWithDefaults(Map<String, Object> generatorProperties) {

        BallerinaProjectToolConfig ballerinaProjectToolConfig = (BallerinaProjectToolConfig) generatorProperties.get("config");
        BallerinaService ballerinaService = (BallerinaService) generatorProperties.get("service");
        HashMap<String, String> dependencies = (HashMap<String, String>) generatorProperties.get("dependencies");
        ballerinaService.addImport(dependencies.get("basePackage"));
        ballerinaService.addImport(dependencies.get("servicePackage"));
        ballerinaService.addImport(dependencies.get("dependentPackage"));
        ballerinaService.setOperationConfigs(ballerinaProjectToolConfig.getOperationConfig());
        return ballerinaService;
    }

    private TemplateContext createTemplateContextForBalService(Map<String, Object> generatorProperties) {
        TemplateContext templateContext = this.getNewTemplateContext();
        BallerinaService ballerinaService = initializeServiceWithDefaults(generatorProperties);
        if (generatorProperties.containsKey("servicePort")) {
            ballerinaService.setPort((Integer) generatorProperties.get("servicePort"));
        }
        templateContext.setProperty("service", ballerinaService);
        if (generatorProperties.containsKey("interactionMethods")) {
            Map<String, String> interactionMethods = (Map<String, String>) generatorProperties.get("interactionMethods");
            interactionMethods.forEach((key, value) -> {
                ballerinaService.getFhirInteractionMethodsContent().setInteractionContentByType(key, value);
            });
        }
        templateContext.setProperty("basePackageImportIdentifier", generatorProperties.get("basePackageImportIdentifier"));
        templateContext.setProperty("servicePackageImportIdentifier", generatorProperties.get("servicePackageImportIdentifier"));
        templateContext.setProperty("dependentPackageImportIdentifier", generatorProperties.get("dependentPackageImportIdentifier"));
        //default api config
        templateContext.setProperty("apiConfName", "apiConfig");
        if (generatorProperties.containsKey("apiConfName")) {
            templateContext.setProperty("apiConfName", generatorProperties.get("apiConfName"));
        }
        templateContext.setProperty("currentYear", Calendar.getInstance().get(Calendar.YEAR));
        return templateContext;
    }
}
