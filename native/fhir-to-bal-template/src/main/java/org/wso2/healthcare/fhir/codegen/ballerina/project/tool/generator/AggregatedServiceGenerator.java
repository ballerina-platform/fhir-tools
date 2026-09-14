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

import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.AggregatedService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.FHIRProfile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Generator for aggregated Ballerina service files containing multiple FHIR APIs.
 */
public class AggregatedServiceGenerator extends AbstractFHIRTemplateGenerator {

    public AggregatedServiceGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = generatorProperties.get("projectAPIPath") + File.separator;
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                BallerinaProjectConstants.RESOURCE_PATH_SEPERATOR + "aggregatedBalService.vm", 
                createTemplateContextForAggregatedBalService(generatorProperties), 
                directoryPath, "service.bal");
    }

    private AggregatedService initializeAggregatedServiceWithDefaults(Map<String, Object> generatorProperties) {
        BallerinaProjectToolConfig ballerinaProjectToolConfig = (BallerinaProjectToolConfig) generatorProperties.get("config");
        AggregatedService aggregatedService = (AggregatedService) generatorProperties.get("aggregatedService");
        HashMap<String, String> dependencies = (HashMap<String, String>) generatorProperties.get("dependencies");
        
        aggregatedService.addImport(dependencies.get("basePackage"));
        aggregatedService.addImport(dependencies.get("servicePackage"));
        if (ballerinaProjectToolConfig.getIncludedIGConfigs().size() <= 1) {
            // Multi-IG: every profile already resolves to its own package via the loop below: nothing in the
            // generated service references the tool-wide default, so importing it here would be an unused
            // import (a Ballerina compile error, not just a warning).
            aggregatedService.addImport(dependencies.get("dependentPackage"));
        }
        // Only multi-IG needs per-profile imports (each IG keeps its own real name/importStatement -- see
        // AbstractBallerinaProjectTool.populateIGs()). For a single IncludedIGConfig entry,
        // profile.getImportsList() is unreliable regardless of embed vs. explicit dependent package:
        // populateIGs() rewrites that IG's importStatement to <project org>/<versionConfig namePrefix>, which
        // is a different string (different org, or a local-module path) than the correct, already-added
        // dependencies.get("dependentPackage") -- adding it too causes a duplicate/conflicting import.
        if (ballerinaProjectToolConfig.getIncludedIGConfigs().size() > 1) {
            for (BallerinaService service : aggregatedService.getServices().values()) {
                for (FHIRProfile profile : service.getProfileList()) {
                    for (Object importStatement : profile.getImportsList()) {
                        aggregatedService.addImport((String) importStatement);
                    }
                }
            }
        }
        aggregatedService.setOperationConfigs(ballerinaProjectToolConfig.getOperationConfig());
        
        return aggregatedService;
    }

    private TemplateContext createTemplateContextForAggregatedBalService(Map<String, Object> generatorProperties) {
        TemplateContext templateContext = this.getNewTemplateContext();
        AggregatedService aggregatedService = initializeAggregatedServiceWithDefaults(generatorProperties);
        
        templateContext.setProperty("aggregatedService", aggregatedService);
        templateContext.setProperty("licenseYear", BallerinaProjectConstants.LICENSE_YEAR);
        templateContext.setProperty("basePackageImportIdentifier", generatorProperties.get("basePackageImportIdentifier"));
        templateContext.setProperty("servicePackageImportIdentifier", generatorProperties.get("servicePackageImportIdentifier"));
        templateContext.setProperty("dependentPackageImportIdentifier", generatorProperties.get("dependentPackageImportIdentifier"));
        
        return templateContext;
    }
}
