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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.DependencyConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.IGTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.PackageTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.SearchParameter;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRSearchParamDef;

import java.util.*;

/**
 * Generator class for package related context
 */
public class PackageContextGenerator {

    private static final Log LOG = LogFactory.getLog(PackageContextGenerator.class);
    private final BallerinaPackageGenToolConfig toolConfig;
    private final Set<String> dataTypesRegistry;
    private PackageTemplateContext packageContext;

    public PackageContextGenerator(BallerinaPackageGenToolConfig config,
                                   Map<String, FHIRImplementationGuide> igEntries) {
        LOG.debug("Package Context Generator Initiated");
        this.toolConfig = config;
        this.dataTypesRegistry = new HashSet<>();
        populatePackageContext(igEntries);
    }

    /**
     * Populate package context
     *
     * @param igEntries   available IGs map
     */
    private void populatePackageContext(Map<String, FHIRImplementationGuide> igEntries) {
        LOG.debug("Started: Package Context population");
        for (Map.Entry<String, FHIRImplementationGuide> entry : igEntries.entrySet()) {
            this.packageContext = new PackageTemplateContext();

            if (toolConfig.getPackageConfig().getBasePackage() != null) {
                this.packageContext.setBasePackageName(toolConfig.getPackageConfig().getBasePackage());
            }

            //update package name based on IG name
            String igKey = entry.getKey();
            if (!igKey.equals(entry.getValue().getName())){
                //IG name updated while parsing; update package name
                if (igKey.equals(toolConfig.getPackageConfig().getName())){
                    //user haven't overridden the package name
                    LOG.debug("IG name updated while parsing; update package name");
                    toolConfig.getPackageConfig().setName(entry.getValue().getName().toLowerCase(Locale.ENGLISH));
                }
            }

            Map<String, String> dependencyMap = new HashMap<>();
            for (DependencyConfig dependencyConfig : toolConfig.getPackageConfig().getDependencyConfigList()) {
                dependencyMap.put(dependencyConfig.getName(), dependencyConfig.getOrg() + "/" + dependencyConfig.getName());
            }
            this.packageContext.setDependenciesMap(dependencyMap);

            FHIRImplementationGuide implementationGuide = entry.getValue();

            populateResourceTemplateContext(implementationGuide);
            populateIGTemplateContexts(entry.getValue().getName(), implementationGuide);
        }
        LOG.debug("Ended: Package Context population");
    }

    /**
     * Initiate resource template context generator
     *
     * @param ig implementation guide DTO
     */
    private void populateResourceTemplateContext(FHIRImplementationGuide ig) {
        LOG.debug("Started: Resource Template Context population");
        ResourceContextGenerator resourceContextGenerator = new ResourceContextGenerator(this.toolConfig, ig,
                this.dataTypesRegistry);
        this.packageContext.setResourceTemplateContextMap(resourceContextGenerator.getResourceTemplateContextMap());
        this.packageContext.setResourceNameTypeMap(resourceContextGenerator.getResourceNameTypeMap());
        this.packageContext.setDataTypesRegistry(resourceContextGenerator.getDataTypesRegistry());
        LOG.debug("Ended: Resource Template Context population");
    }

    /**
     * Populate IG template contexts
     *
     * @param igCode              given code for the IG
     * @param implementationGuide implementation guide DTO
     */
    private void populateIGTemplateContexts(String igCode, FHIRImplementationGuide implementationGuide) {
        LOG.debug("Started: IG Template Context population");
        IGTemplateContext igTemplateContext = new IGTemplateContext();
        String igName = toolConfig.getPackageConfig().getName().replaceAll("\\.", "_");
        //todo: handle the case where IG resource exist in the spec dir
        igTemplateContext.setIgName(igName);
        igTemplateContext.setTitle(igName);
        igTemplateContext.setIgCode(igCode.replaceAll("\\.", "_"));
        this.packageContext.setIgTemplateContext(igTemplateContext);
        populateSearchParameters(implementationGuide);
        LOG.debug("Ended: IG Template Context population");
    }

    /**
     * Populate IG all search parameters
     *
     * @param implementationGuide implementation guide
     */
    private void populateSearchParameters(FHIRImplementationGuide implementationGuide) {
        LOG.debug("Started: Search Parameter population");
        HashMap<String, Map<String, SearchParameter>> searchParameterMap = new HashMap<>();

        for (Map.Entry<String, FHIRSearchParamDef> searchParamEntry : implementationGuide.getSearchParameters().entrySet()) {
            Map<String, SearchParameter> searchParameterTypeMap;
            SearchParameter searchParameter;
            String searchParamName = searchParamEntry.getValue().getSearchParameter().getName();
            String searchParamType = searchParamEntry.getValue().getSearchParameter().getType().name();
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
            for (String base : searchParamEntry.getValue().getBaseResources()) {
                if (this.packageContext.getResourceNameTypeMap().containsValue(base)) {
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
                    expression = expression + " | " + searchParamEntry.getValue().getSearchParameter().getExpression();
                    searchParameter.setExpression(expression);
                } else {
                    searchParameter.setExpression(searchParamEntry.getValue().getSearchParameter().getExpression());
                }
                searchParameterTypeMap.put(searchParamDerivedName, searchParameter);
                searchParameterMap.put(searchParamName, searchParameterTypeMap);
            }
        }
        this.packageContext.getIgTemplateContext().setSearchParameters(searchParameterMap);
        LOG.debug("Ended: Search Parameter population");
    }

    public PackageTemplateContext getPackageContext() {
        return packageContext;
    }
}
