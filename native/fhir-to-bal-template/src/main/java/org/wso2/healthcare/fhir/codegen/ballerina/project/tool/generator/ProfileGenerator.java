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

import org.wso2.healthcare.codegen.tooling.common.core.TemplateContext;
import org.wso2.healthcare.codegen.tooling.common.core.ToolContext;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.FHIRProfile;
import org.wso2.healthcare.fhir.codegen.tool.lib.AbstractFHIRTemplateGenerator;

import java.io.File;
import java.util.Map;

/**
 * Generator class for profile specific source connection file.
 */
public class ProfileGenerator extends AbstractFHIRTemplateGenerator {
    public ProfileGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = this.getTargetDir() + generatorProperties.get("resourceType") + "API" + File.separator;
        BallerinaService ballerinaService = (BallerinaService) generatorProperties.get("service");
        for (FHIRProfile profile : ballerinaService.getProfileList()) {
            generatorProperties.put("currentProfile", profile);
            String fileName = profile.getIgName().toLowerCase() + "_" + profile.getName().toLowerCase() + "_connect.bal";
            this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                    File.separator + "profileConnect.vm", createTemplateContextForProfileImpl(
                    generatorProperties), directoryPath, fileName);
        }
    }

    private TemplateContext createTemplateContextForProfileImpl(Map<String, Object> generatorProperties) {
        TemplateContext templateContext = this.getNewTemplateContext();
        FHIRProfile currentProfile = (FHIRProfile) generatorProperties.get("currentProfile");
        currentProfile.addImport("wso2healthcare/healthcare.fhir.r4");
        currentProfile.addImport("ballerina/http");
        templateContext.setProperty("profile", currentProfile);
        return templateContext;
    }
}
