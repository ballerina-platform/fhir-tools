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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.core.SpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.DependencyConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.PackageTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class for generating package context for Ballerina FHIR package generation.
 * This class is responsible for populating the package context.
 * Extended by R4PackageContextGenerator and R5PackageContextGenerator.
 */

public abstract class AbstractPackageContextGenerator {
    private static final Log LOG = LogFactory.getLog(AbstractPackageContextGenerator.class);
    private final BallerinaPackageGenToolConfig toolConfig;
    private PackageTemplateContext packageContext;

    public AbstractPackageContextGenerator(BallerinaPackageGenToolConfig config, Map<String, FHIRImplementationGuide> igEntries,
                                           SpecificationData specificationData) {
        this.toolConfig = config;
        GeneratorUtils.getInstance().setToolConfig(config);
        populatePackageContext(igEntries, (FHIRSpecificationData) specificationData);
    }

    /**
     * Populate package context
     *
     * @param igEntries         available IGs map
     * @param specificationData specification data
     */
    private void populatePackageContext(Map<String, FHIRImplementationGuide> igEntries,
                                        FHIRSpecificationData specificationData) {
        LOG.debug("Started: Package Context population");
        for (Map.Entry<String, FHIRImplementationGuide> entry : igEntries.entrySet()) {
            this.packageContext = new PackageTemplateContext();

            if (toolConfig.getPackageConfig().getBasePackage() != null) {
                this.packageContext.setBasePackageName(toolConfig.getPackageConfig().getBasePackage());
            }

            if (toolConfig.getPackageConfig().getInternationalPackage() != null) {
                this.packageContext.setInternationalPackageName(toolConfig.getPackageConfig().getInternationalPackage());
            }

            Map<String, String> dependencyMap = new HashMap<>();
            for (DependencyConfig dependencyConfig : toolConfig.getPackageConfig().getDependencyConfigList()) {
                dependencyMap.put(dependencyConfig.getName(), dependencyConfig.getOrg() + "/" + dependencyConfig.getName());
            }
            this.packageContext.setDependenciesMap(dependencyMap);

            FHIRImplementationGuide implementationGuide = entry.getValue();

            populateDatatypeTemplateContext(specificationData);
            populateResourceTemplateContext(implementationGuide);
            populateExtensionTemplateContext(specificationData);
            populateIGTemplateContexts(entry.getValue().getName(), implementationGuide);
        }
        LOG.debug("Ended: Package Context population");
    }

    public PackageTemplateContext getPackageContext() {
        return packageContext;
    }

    public BallerinaPackageGenToolConfig getToolConfig() {
        return toolConfig;
    }

    protected abstract void populateDatatypeTemplateContext(FHIRSpecificationData specificationData);

    protected abstract void populateExtensionTemplateContext(FHIRSpecificationData specificationData);

    protected abstract void populateResourceTemplateContext(FHIRImplementationGuide implementationGuide);

    protected abstract void populateIGTemplateContexts(String igCode, FHIRImplementationGuide implementationGuide);

    protected abstract void populateSearchParameters(FHIRImplementationGuide implementationGuide);
}
