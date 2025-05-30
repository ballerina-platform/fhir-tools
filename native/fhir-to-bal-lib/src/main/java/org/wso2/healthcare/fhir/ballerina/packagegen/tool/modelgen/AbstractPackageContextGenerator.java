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

            Map<String, String> dependencyMap = new HashMap<>();
            for (DependencyConfig dependencyConfig : toolConfig.getPackageConfig().getDependencyConfigList()) {
                dependencyMap.put(dependencyConfig.getName(), dependencyConfig.getOrg() + "/" + dependencyConfig.getName());
            }
            this.packageContext.setDependenciesMap(dependencyMap);

            FHIRImplementationGuide implementationGuide = entry.getValue();

            populateDatatypeTemplateContext(specificationData);
            populateResourceTemplateContext(implementationGuide);
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

    protected abstract void populateResourceTemplateContext(FHIRImplementationGuide implementationGuide);

    protected abstract void populateIGTemplateContexts(String igCode, FHIRImplementationGuide implementationGuide);

    protected abstract void populateSearchParameters(FHIRImplementationGuide implementationGuide);
}
