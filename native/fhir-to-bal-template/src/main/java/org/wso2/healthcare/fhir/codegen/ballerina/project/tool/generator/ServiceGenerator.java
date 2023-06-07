/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import org.wso2.healthcare.codegen.tooling.common.core.TemplateContext;
import org.wso2.healthcare.codegen.tooling.common.core.ToolContext;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.InteractionConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.ResourceMethod;
import org.wso2.healthcare.fhir.codegen.tool.lib.AbstractFHIRTemplateGenerator;

import java.io.File;
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
        String directoryPath = this.getTargetDir() + generatorProperties.get("resourceType") + "API" + File.separator;
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                File.separator + "balService.vm", createTemplateContextForBalService(generatorProperties),
                directoryPath, "service.bal");
    }

    private BallerinaService initializeServiceWithDefaults(Map<String, Object> generatorProperties) {

        BallerinaProjectToolConfig ballerinaProjectToolConfig = (BallerinaProjectToolConfig) generatorProperties.get(
                "config");
        String resourceName = ((String) generatorProperties.get("resourceType"));
        BallerinaService ballerinaService = (BallerinaService) generatorProperties.get("service");
        ballerinaService.addImport("wso2healthcare/healthcare.fhir.r4");
        ballerinaService.addImport("ballerina/log");
        ballerinaService.addImport("ballerina/http");

        for (InteractionConfig interactionConfig : ballerinaProjectToolConfig.getInteractionConfigs()) {
            ResourceMethod resourceMethod = null;
            switch (interactionConfig.getName()) {
                case "search":
                    resourceMethod = new ResourceMethod("search", resourceName, "get");
                    resourceMethod.setDescriptionComment("// Search the resource type based on some filter criteria");
                    ballerinaService.addInterceptor("FHIRSearchRequestInterceptor");
                    break;
                case "read":
                    resourceMethod = new ResourceMethod("read", resourceName + "/[string id]", "get");
                    resourceMethod.setDescriptionComment("// Read the current state of the resource");
                    ballerinaService.addInterceptor("FHIRReadRequestInterceptor");
                    break;
                case "create":
                    resourceMethod = new ResourceMethod("create", resourceName, "post");
                    resourceMethod.setDescriptionComment("// Create a new resource with a server assigned id");
                    ballerinaService.addInterceptor("FHIRCreateRequestInterceptor");
                    break;
                case "update":
                    resourceMethod = new ResourceMethod("update", resourceName + "/[string id]", "put");
                    resourceMethod.setDescriptionComment(
                            "// Update an existing resource by its id (or create it if it is new)");
                    ballerinaService.addInterceptor("FHIRUpdateRequestInterceptor");
                    break;
                case "patch":
                    resourceMethod = new ResourceMethod("patch", resourceName + "/[string id]", "patch");
                    resourceMethod.setDescriptionComment(
                            "// Update an existing resource by posting a set of changes to it");
                    ballerinaService.addInterceptor("FHIRPatchRequestInterceptor");
                    break;
                case "delete":
                    resourceMethod = new ResourceMethod("delete", resourceName + "/[string id]", "delete");
                    resourceMethod.setDescriptionComment("// Delete an existing resource by its id");
                    ballerinaService.addInterceptor("FHIRDeleteRequestInterceptor");
                    break;
                case "history":
                    resourceMethod = new ResourceMethod("history", resourceName + "/[string id]/_history", "get");
                    resourceMethod.setDescriptionComment("// Retrieve the change history for a particular resource");
                    ballerinaService.addInterceptor("FHIRInstanceHistorySearchRequestInterceptor");
                    break;
                case "history-instance":
                    resourceMethod = new ResourceMethod("history", resourceName +
                            "/[string id]/_history/[string vid]", "get");
                    resourceMethod.setDescriptionComment(
                            "// Retrieve the change history for a particular resource type");
                    ballerinaService.addInterceptor("FHIRInstanceHistorySearchRequestInterceptor");
                    break;
                case "history-type":
                    resourceMethod = new ResourceMethod("history", resourceName +
                            "/_history", "get");
                    resourceMethod.setDescriptionComment(
                            "// Retrieve the change history for a particular resource type");
                    ballerinaService.addInterceptor("FHIRTypeHistorySearchRequestInterceptor");
                    break;
                case "history-system":
                    resourceMethod = new ResourceMethod("history", "/_history", "get");
                    resourceMethod.setDescriptionComment(
                            "// Retrieve the change history for a particular resource type");
                    ballerinaService.addInterceptor("FHIRSystemHistorySearchRequestInterceptor");
                    break;
                default:
                    break;
            }
            ballerinaService.addResourceMethod(resourceMethod);
        }

        //todo: validate operations and search parameters against the resource type
        ballerinaService.setOperationConfigs(ballerinaProjectToolConfig.getOperationConfig());
        return ballerinaService;
    }

    private TemplateContext createTemplateContextForBalService(Map<String, Object> generatorProperties) {

        TemplateContext templateContext = this.getNewTemplateContext();
        BallerinaService ballerinaService = initializeServiceWithDefaults(generatorProperties);
        templateContext.setProperty("service", ballerinaService);
        return templateContext;
    }
}
