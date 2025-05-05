package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.versions.r5;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.core.SpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.DataTypesRegistry;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.AbstractPackageContextGenerator;

import java.util.Map;

public class R5PackageContextGenerator extends AbstractPackageContextGenerator {
    private static final Log LOG = LogFactory.getLog(R5PackageContextGenerator.class);

    public R5PackageContextGenerator(BallerinaPackageGenToolConfig config, Map<String, FHIRImplementationGuide> igEntries,
                                     SpecificationData specificationData){
        super(config, igEntries, specificationData);
    }

    @Override
    protected void populateDatatypeTemplateContext(FHIRSpecificationData specificationData) {
        LOG.debug("Started: Datatype Template Context population");
        R5DatatypeContextGenerator r5DatatypeContextGenerator = new R5DatatypeContextGenerator(specificationData);
        getPackageContext().setDatatypeTemplateContextMap(r5DatatypeContextGenerator.getDataTypeTemplateContextMap());
        LOG.debug("Ended: Datatype Template Context population");
    }

    @Override
    protected void populateResourceTemplateContext(FHIRImplementationGuide ig) {
        LOG.debug("Started: Resource Template Context population");
        R5ResourceContextGenerator r5ResourceContextGenerator = new R5ResourceContextGenerator(getToolConfig(), ig, getPackageContext().getDatatypeTemplateContextMap());
        getPackageContext().setResourceTemplateContextMap(r5ResourceContextGenerator.getResourceTemplateContextMap());
        getPackageContext().setResourceNameTypeMap(r5ResourceContextGenerator.getResourceNameTypeMap());
        getPackageContext().setDataTypesRegistry(DataTypesRegistry.getInstance().getDataTypesRegistry());
        LOG.debug("Ended: Resource Template Context population");
    }

    @Override
    protected void populateIGTemplateContexts(String igName, FHIRImplementationGuide implementationGuide) {

    }

    @Override
    protected void populateSearchParameters(FHIRImplementationGuide implementationGuide) {

    }
}
