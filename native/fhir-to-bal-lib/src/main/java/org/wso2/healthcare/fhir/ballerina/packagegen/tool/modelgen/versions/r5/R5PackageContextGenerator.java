package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.versions.r5;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.SearchParameter;
import org.wso2.healthcare.codegen.tool.framework.commons.core.SpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRSearchParamDef;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r5.model.FHIRR5SearchParamDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.DataTypesRegistry;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.IGTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.AbstractPackageContextGenerator;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
    protected void populateIGTemplateContexts(String igCode, FHIRImplementationGuide implementationGuide) {
        LOG.debug("Started: IG Template Context population");
        IGTemplateContext igTemplateContext = new IGTemplateContext();
        String igName = getToolConfig().getPackageConfig().getName().replaceAll("\\.", "_");
        igTemplateContext.setIgName(igName);
        igTemplateContext.setTitle(igName);
        igTemplateContext.setIgCode(igCode.replaceAll("\\.", "_"));
        getPackageContext().setIgTemplateContext(igTemplateContext);
        populateSearchParameters(implementationGuide);
        LOG.debug("Ended: IG Template Context population");
    }

    @Override
    protected void populateSearchParameters(FHIRImplementationGuide implementationGuide) {
        LOG.debug("Started: Search Parameter population");
        HashMap<String, Map<String, SearchParameter>> searchParameterMap = new HashMap<>();

        for(Map.Entry<String, FHIRSearchParamDef> searchParamEntry : implementationGuide.getSearchParameters().entrySet()){
            Map<String, SearchParameter> searchParameterTypeMap;
            SearchParameter searchParameter;

            FHIRR5SearchParamDef fhirSearchParamDef = (FHIRR5SearchParamDef) searchParamEntry.getValue();
            String searchParamName = fhirSearchParamDef.getSearchParameter().getName();
            String searchParamType = fhirSearchParamDef.getSearchParameter().getType().name();
            String searchParamDerivedName = searchParamName + searchParamType;

            if(searchParameterMap.containsKey(searchParamName)){
                searchParameterTypeMap = searchParameterMap.get(searchParamName);

                if(searchParameterTypeMap.containsKey(searchParamDerivedName)){
                    searchParameter = searchParameterTypeMap.get(searchParamDerivedName);
                }
                else{
                    searchParameter = new SearchParameter();
                }
            }
            else{
                searchParameterTypeMap = new HashMap<>();
                searchParameter = new SearchParameter();
            }

            ArrayList<String> bases = new ArrayList<>();
            for(String base : fhirSearchParamDef.getBaseResources()){
                if(getPackageContext().getResourceNameTypeMap().containsValue(base)){
                    if(!bases.contains(base)){
                        bases.add(base);
                    }
                }
                else if(base.equals("Resource")){
                    bases.add("Resource");
                }
            }

            if(!bases.isEmpty()){
                searchParameter.setName(searchParamName);
                searchParameter.setType(searchParamType);

                if(searchParameter.getBase() != null){
                    bases.addAll(searchParameter.getBase());
                }

                searchParameter.setBase(bases);

                if(searchParameter.getExpression() != null){
                    String expression = searchParameter.getExpression();
                    expression = expression + " | " + fhirSearchParamDef.getSearchParameter().getExpression();
                    searchParameter.setExpression(expression);
                }
                else{
                    searchParameter.setExpression(fhirSearchParamDef.getSearchParameter().getExpression());
                }
                searchParameterTypeMap.put(searchParamDerivedName, searchParameter);
                searchParameterMap.put(searchParamName, searchParameterTypeMap);
            }
        }
        getPackageContext().getIgTemplateContext().setSearchParameters(searchParameterMap);
        LOG.debug("Ended: Search Parameter population");
    }
}
