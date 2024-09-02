/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.cds.codegen.ballerina.tool.generator;

import org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants;
import org.wso2.healthcare.cds.codegen.ballerina.tool.model.BallerinaService;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;

import java.util.Map;

import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_DECISION_SYSTEM_CONNECTION_FILE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_DECISION_SYSTEM_CONNECTION_VM;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_FEEDBACK_SYSTEM_CONNECTION_FILE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_FEEDBACK_SYSTEM_CONNECTION_VM;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_INTERCEPTOR_FILE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_INTERCEPTOR_VM;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_SERVICE_FILE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_SERVICE_VM;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_UTILS_FILE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_UTILS_VM;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.SERVICE;


/**
 * Generator for Ballerina service files.
 */
public class ServiceGenerator extends AbstractFHIRTemplateGenerator {

    public ServiceGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = getTargetDir();
        TemplateContext templateContext = createTemplateContextForBalService(generatorProperties);

        this.getTemplateEngine().generateOutputAsFile(
                CdsBallerinaProjectConstants.RESOURCE_PATH_TEMPLATES + CdsBallerinaProjectConstants.RESOURCE_PATH_SEPARATOR + BAL_SERVICE_VM,
                templateContext,
                directoryPath,
                BAL_SERVICE_FILE);
        this.getTemplateEngine().generateOutputAsFile(
                CdsBallerinaProjectConstants.RESOURCE_PATH_TEMPLATES + CdsBallerinaProjectConstants.RESOURCE_PATH_SEPARATOR + BAL_INTERCEPTOR_VM,
                templateContext,
                directoryPath,
                BAL_INTERCEPTOR_FILE);
        this.getTemplateEngine().generateOutputAsFile(
                CdsBallerinaProjectConstants.RESOURCE_PATH_TEMPLATES + CdsBallerinaProjectConstants.RESOURCE_PATH_SEPARATOR + BAL_UTILS_VM,
                templateContext,
                directoryPath,
                BAL_UTILS_FILE);
        this.getTemplateEngine().generateOutputAsFile(CdsBallerinaProjectConstants.RESOURCE_PATH_TEMPLATES + CdsBallerinaProjectConstants.RESOURCE_PATH_SEPARATOR + BAL_DECISION_SYSTEM_CONNECTION_VM,
                templateContext,
                directoryPath,
                BAL_DECISION_SYSTEM_CONNECTION_FILE);
        this.getTemplateEngine().generateOutputAsFile(
                CdsBallerinaProjectConstants.RESOURCE_PATH_TEMPLATES + CdsBallerinaProjectConstants.RESOURCE_PATH_SEPARATOR + BAL_FEEDBACK_SYSTEM_CONNECTION_VM,
                templateContext,
                directoryPath,
                BAL_FEEDBACK_SYSTEM_CONNECTION_FILE);
    }

    private TemplateContext createTemplateContextForBalService(Map<String, Object> generatorProperties) {
        TemplateContext templateContext = this.getNewTemplateContext();
        BallerinaService ballerinaService = (BallerinaService) generatorProperties.get(SERVICE);
        templateContext.setProperty(SERVICE, ballerinaService);
        return templateContext;
    }
}
