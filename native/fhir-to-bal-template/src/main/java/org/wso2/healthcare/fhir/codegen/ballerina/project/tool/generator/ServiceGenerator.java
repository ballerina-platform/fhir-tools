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
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.InteractionConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.ResourceMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                File.separator + "balService.vm", createTemplateContextForBalService(
                generatorProperties), directoryPath, "service.bal");
    }

    private BallerinaService initializeServiceWithDefaults(Map<String, Object> generatorProperties) {

        BallerinaProjectToolConfig ballerinaProjectToolConfig = (BallerinaProjectToolConfig) generatorProperties.get("config");
        String resourceName = ((String) generatorProperties.get("resourceType"));
        BallerinaService ballerinaService = (BallerinaService) generatorProperties.get("service");
        HashMap<String, String> dependencies = (HashMap<String, String>) generatorProperties.get("dependencies");
        ballerinaService.addImport(dependencies.get("basePackage"));
        ballerinaService.addImport(dependencies.get("servicePackage"));
        ballerinaService.addImport(dependencies.get("resourcePackage"));

        String basePackageIdentifier = (String) generatorProperties.get("basePackageImportIdentifier");
        String httpMethod = "get";
        String returnType = String.format("%s|%s:FHIRError", resourceName, basePackageIdentifier);
        ArrayList<String> params;
        String methodDescription = "";

        for (InteractionConfig interactionConfig : ballerinaProjectToolConfig.getInteractionConfigs()) {
            params = new ArrayList<>(Collections.singletonList(String.format("%s:FHIRContext fhirContext", basePackageIdentifier)));
            switch (interactionConfig.getName()) {
                case "read":
                    methodDescription = BallerinaProjectConstants.READ_METHOD_DESC;
                    break;
                case "vread":
                    methodDescription = BallerinaProjectConstants.VREAD_METHOD_DESC;
                    break;
                case "search":
                    returnType = String.format("%s:%s|%s:FHIRError", basePackageIdentifier, "Bundle", basePackageIdentifier);
                    methodDescription = BallerinaProjectConstants.SEARCH_TYPE_METHOD_DESC;
                    break;
                case "create":
                    httpMethod = "post";
                    params.add(String.format("%s %s", resourceName, resourceName.toLowerCase()));
                    methodDescription = BallerinaProjectConstants.CREATE_METHOD_DESC;
                    break;
                case "update":
                    httpMethod = "put";
                    params.add(String.format("%s %s", resourceName, resourceName.toLowerCase()));
                    methodDescription = BallerinaProjectConstants.UPDATE_METHOD_DESC;
                    break;
                case "patch":
                    httpMethod = "patch";
                    params.add(String.format("%s patch", "json"));
                    methodDescription = BallerinaProjectConstants.PATCH_METHOD_DESC;
                    break;
                case "delete":
                    httpMethod = "delete";
                    methodDescription = BallerinaProjectConstants.DELETE_METHOD_DESC;
                    break;
                case "history-instance":
                    returnType = String.format("%s:%s|%s:FHIRError", basePackageIdentifier, "Bundle", basePackageIdentifier);
                    methodDescription = BallerinaProjectConstants.HISTORY_INSTANCE_METHOD_DESC;
                    break;
                case "history-type":
                    returnType = String.format("%s:%s|%s:FHIRError", basePackageIdentifier, "Bundle", basePackageIdentifier);
                    methodDescription = BallerinaProjectConstants.HISTORY_TYPE_METHOD_DESC;
                    break;
                default:
                    break;
            }
            ResourceMethod resourceMethod = new ResourceMethod(interactionConfig.getName(), resourceName, params, httpMethod,
                    returnType, methodDescription);
            ballerinaService.addResourceMethod(resourceMethod);
        }

        ballerinaService.setOperationConfigs(ballerinaProjectToolConfig.getOperationConfig());
        return ballerinaService;
    }

    private TemplateContext createTemplateContextForBalService(Map<String, Object> generatorProperties) {
        TemplateContext templateContext = this.getNewTemplateContext();
        BallerinaService ballerinaService = initializeServiceWithDefaults(generatorProperties);
        templateContext.setProperty("service", ballerinaService);
        templateContext.setProperty("basePackageImportIdentifier", generatorProperties.get("basePackageImportIdentifier"));
        templateContext.setProperty("servicePackageImportIdentifier", generatorProperties.get("servicePackageImportIdentifier"));
        templateContext.setProperty("resourcePackageImportIdentifier", generatorProperties.get("resourcePackageImportIdentifier"));
        return templateContext;
    }
}
