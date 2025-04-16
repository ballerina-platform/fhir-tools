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
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.PackageContextGenerator;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.templategen.PackageTemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTool;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRDataTypeDef;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;

import java.util.HashMap;
import java.util.Map;

/**
 * Ballerina FHIR Package Generator Tool.
 */
public class BallerinaPackageGenTool extends AbstractFHIRTool {
    private final Map<String, FHIRImplementationGuide> enabledIgs = new HashMap<>();
    private BallerinaPackageGenToolConfig packageGenToolConfig;

    @Override
    public void initialize(ToolConfig toolConfig) {
        this.packageGenToolConfig = (BallerinaPackageGenToolConfig) toolConfig;
    }

    @Override
    public TemplateGenerator execute(ToolContext toolContext) throws CodeGenException {

        enabledIgs.putAll(((FHIRSpecificationData) toolContext.getSpecificationData()).getFhirImplementationGuides());
        for (String igName : enabledIgs.keySet()) {
            int resourceCount = ((FHIRSpecificationData) toolContext.getSpecificationData()).
                    getFhirImplementationGuides().get(igName).getResources().keySet().size();
            if (resourceCount < 1) {
                throw new CodeGenException("No resources found in the IG: " + igName);
            }

        }

        if (packageGenToolConfig.isEnabled()) {
            String targetRoot = packageGenToolConfig.getTargetDir();
            Map<String, FHIRDataTypeDef> dataTypes = ((FHIRSpecificationData) toolContext.getSpecificationData()).getDataTypes();
            PackageContextGenerator packageContextGenerator = new PackageContextGenerator(
                    packageGenToolConfig,
                    enabledIgs, toolContext.getSpecificationData());

            PackageTemplateGenerator packageTemplateGenerator = new PackageTemplateGenerator(targetRoot);

            Map<String, Object> properties = new HashMap<>();
            properties.put("toolConfig", packageGenToolConfig);
            if (packageContextGenerator.getPackageContext() == null) {
                throw new CodeGenException("Package context is not available.");
            }
            properties.put("packageContext", packageContextGenerator.getPackageContext());
            properties.put("datatypeContext", packageContextGenerator.getPackageContext().getDatatypeTemplateContextMap());
            packageTemplateGenerator.setGeneratorProperties(properties);
            return packageTemplateGenerator;
        }
        return null;
    }
}
