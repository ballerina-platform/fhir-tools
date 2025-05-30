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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.versions.r4;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.core.SpecificationData;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.SearchParameter;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r4.model.FHIRR4SearchParamDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.DataTypesRegistry;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.IGTemplateContext;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRSearchParamDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.AbstractPackageContextGenerator;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Generator class for package related context
 */
public class R4PackageContextGenerator extends AbstractPackageContextGenerator {

    private static final Log LOG = LogFactory.getLog(R4PackageContextGenerator.class);

    public R4PackageContextGenerator(BallerinaPackageGenToolConfig config, Map<String, FHIRImplementationGuide> igEntries,
                                     SpecificationData specificationData) {
       super(config, igEntries, specificationData);
    }

    @Override
    protected void populateDatatypeTemplateContext(FHIRSpecificationData specificationData) {
        LOG.debug("Started: Datatype Template Context population");
        R4DatatypeContextGenerator r4DatatypeContextGenerator = new R4DatatypeContextGenerator(specificationData);
        getPackageContext().setDatatypeTemplateContextMap(r4DatatypeContextGenerator.getDataTypeTemplateContextMap());
        LOG.debug("Ended: Datatype Template Context population");
    }

    /**
     * Initiate resource template context generator
     *
     * @param ig implementation guide DTO
     */
    @Override
    protected void populateResourceTemplateContext(FHIRImplementationGuide ig) {
        LOG.debug("Started: Resource Template Context population");
        R4ResourceContextGenerator r4ResourceContextGenerator = new R4ResourceContextGenerator(getToolConfig(), ig,
                getPackageContext().getDatatypeTemplateContextMap());
        getPackageContext().setResourceTemplateContextMap(r4ResourceContextGenerator.getResourceTemplateContextMap());
        getPackageContext().setResourceNameTypeMap(r4ResourceContextGenerator.getResourceNameTypeMap());
        getPackageContext().setDataTypesRegistry(DataTypesRegistry.getInstance().getDataTypesRegistry());
        LOG.debug("Ended: Resource Template Context population");
    }

    /**
     * Populate IG template contexts
     *
     * @param igCode              given code for the IG
     * @param implementationGuide implementation guide DTO
     */
    @Override
    protected void populateIGTemplateContexts(String igCode, FHIRImplementationGuide implementationGuide) {
        LOG.debug("Started: IG Template Context population");
        IGTemplateContext igTemplateContext = new IGTemplateContext();
        String igName = getToolConfig().getPackageConfig().getName().replaceAll("\\.", "_");
        //todo: handle the case where IG resource exist in the spec dir
        igTemplateContext.setIgName(igName);
        igTemplateContext.setTitle(igName);
        igTemplateContext.setIgCode(igCode.replaceAll("\\.", "_"));
        getPackageContext().setIgTemplateContext(igTemplateContext);
        populateSearchParameters(implementationGuide);
        LOG.debug("Ended: IG Template Context population");
    }

    /**
     * Populate IG all search parameters
     *
     * @param implementationGuide implementation guide
     */
    @Override
    protected void populateSearchParameters(FHIRImplementationGuide implementationGuide) {
        LOG.debug("Started: Search Parameter population");
        HashMap<String, Map<String, SearchParameter>> searchParameterMap = new HashMap<>();

        for (Map.Entry<String, FHIRSearchParamDef> searchParamEntry : implementationGuide.getSearchParameters().entrySet()) {
            Map<String, SearchParameter> searchParameterTypeMap;
            SearchParameter searchParameter;

            FHIRR4SearchParamDef fhirSearchParamDef = (FHIRR4SearchParamDef) searchParamEntry.getValue();
            String searchParamName = fhirSearchParamDef.getSearchParameter().getName();
            String searchParamType = fhirSearchParamDef.getSearchParameter().getType().name();
            String searchParamDerivedName = searchParamName + searchParamType;

            if (searchParameterMap.containsKey(searchParamName)) {
                searchParameterTypeMap = searchParameterMap.get(searchParamName);

                if (searchParameterTypeMap.containsKey(searchParamDerivedName)) {
                    searchParameter = searchParameterTypeMap.get(searchParamDerivedName);
                } else {
                    searchParameter = new SearchParameter();
                }
            } else {
                searchParameterTypeMap = new HashMap<>();
                searchParameter = new SearchParameter();
            }

            ArrayList<String> bases = new ArrayList<>();
            for (String base : fhirSearchParamDef.getBaseResources()) {
                if (getPackageContext().getResourceNameTypeMap().containsValue(base)) {
                    if (!bases.contains(base)) {
                        bases.add(base);
                    }
                } else if (base.equals("Resource")) {
                    bases.add("Resource");
                }
            }

            if (!bases.isEmpty()) {
                searchParameter.setName(searchParamName);
                searchParameter.setType(searchParamType);

                if (searchParameter.getBase() != null)
                    bases.addAll(searchParameter.getBase());

                searchParameter.setBase(bases);

                if (searchParameter.getExpression() != null) {
                    String expression = searchParameter.getExpression();
                    expression = expression + " | " + fhirSearchParamDef.getSearchParameter().getExpression();
                    searchParameter.setExpression(expression);
                } else {
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
