// Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

package org.wso2.healthcare.fhir.ballerina.packagegen.tool;

import org.wso2.healthcare.codegen.tooling.common.config.ToolConfig;
import org.wso2.healthcare.codegen.tooling.common.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tooling.common.core.ToolContext;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.IncludedIGConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.PackageContextGenerator;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.templategen.PackageTemplateGenerator;
import org.wso2.healthcare.fhir.codegen.tool.lib.common.FHIRSpecificationData;
import org.wso2.healthcare.fhir.codegen.tool.lib.config.FHIRToolConfig;
import org.wso2.healthcare.fhir.codegen.tool.lib.config.IGConfig;
import org.wso2.healthcare.fhir.codegen.tool.lib.core.AbstractFHIRTool;
import org.wso2.healthcare.fhir.codegen.tool.lib.core.FHIRToolContext;
import org.wso2.healthcare.fhir.codegen.tool.lib.model.FHIRImplementationGuide;

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

            PackageTemplateGenerator packageTemplateGenerator = new PackageTemplateGenerator(
                    targetDirectory);

            Map<String, Object> properties = new HashMap<>();
            properties.put("toolConfig", packageGenToolConfig);
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
        for (Map.Entry<String, IncludedIGConfig> entry : packageGenToolConfig.getIncludedIGConfigs().entrySet()) {
            String igName = entry.getKey();
            if (entry.getValue().isEnable()) {
                FHIRImplementationGuide ig = ((FHIRSpecificationData) toolContext.getSpecificationData()).
                        getFhirImplementationGuides().get(igName);
                if (ig == null) {
                    //if IG is enabled in the tool config but specification files are not available in the resources.
                    continue;
                }
                enabledIgs.put(igName, allIgs.get(igName));
            }
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
