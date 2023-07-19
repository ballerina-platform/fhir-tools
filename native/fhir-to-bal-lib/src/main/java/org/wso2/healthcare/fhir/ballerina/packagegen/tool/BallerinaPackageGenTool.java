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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool;

import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.IncludedIGConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.PackageContextGenerator;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.templategen.PackageTemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.config.FHIRToolConfig;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.config.IGConfig;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTool;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.FHIRToolContext;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Ballerina FHIR Package Generator Tool.
 */
public class BallerinaPackageGenTool extends AbstractFHIRTool {

    private final Map<String, IGConfig> enabledIgs = new HashMap<>();
    private BallerinaPackageGenToolConfig packageGenToolConfig;

    @Override
    public void initialize(ToolConfig toolConfig) {
        this.packageGenToolConfig = (BallerinaPackageGenToolConfig) toolConfig;
    }

    @Override
    public TemplateGenerator execute(ToolContext toolContext) throws CodeGenException {
        populateEnabledIGs(toolContext);

        if (packageGenToolConfig.isEnabled()) {
            String targetRoot = packageGenToolConfig.getTargetDir();
            String targetDirectory = targetRoot + File.separator + ToolConstants.GENERATION_DIR;

            PackageContextGenerator packageContextGenerator = new PackageContextGenerator(
                    (FHIRToolContext) toolContext,
                    packageGenToolConfig,
                    getEnabledIgs());

            PackageTemplateGenerator packageTemplateGenerator = new PackageTemplateGenerator(targetDirectory);

            Map<String, Object> properties = new HashMap<>();
            properties.put("toolConfig", packageGenToolConfig);
            if (packageContextGenerator.getPackageContext() == null) {
                throw new CodeGenException("Package context is not available.");
            }
            properties.put("packageContext", packageContextGenerator.getPackageContext());
            packageTemplateGenerator.setGeneratorProperties(properties);
            return packageTemplateGenerator;
        }
        return null;
    }

    /**
     * Populate enabled IGs for package generation.
     *
     * @param toolContext included IGs
     */
    private void populateEnabledIGs(ToolContext toolContext) {

        Map<String, IGConfig> allIgs = ((FHIRToolConfig) toolContext.getConfig()).getIgConfigs();
        for (Map.Entry<String, IGConfig> entry : allIgs.entrySet()) {
            String igName = entry.getKey();
            FHIRImplementationGuide ig = ((FHIRSpecificationData) toolContext.getSpecificationData()).
                    getFhirImplementationGuides().get(igName);
            if (ig == null) {
                //if IG is enabled in the tool config but specification files are not available in the resources,
                // skip adding it and continue
                continue;
            }
            enabledIgs.put(igName, entry.getValue());
        }
    }

    /**
     * Getter for enables IGs
     *
     * @return enables IGs
     */
    public Map<String, IGConfig> getEnabledIgs() {
        return enabledIgs;
    }
}
