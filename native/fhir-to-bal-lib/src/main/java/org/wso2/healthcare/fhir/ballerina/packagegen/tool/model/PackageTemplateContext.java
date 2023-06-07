// Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class holder for package related template context
 */
public class PackageTemplateContext {
    private IGTemplateContext igTemplateContext;
    private String basePackageName;
    private Map<String, ResourceTemplateContext> resourceTemplateContextMap;
    private Map<String, String> resourceNameTypeMap;
    private Set<String> dataTypesRegistry;
    private Map<String, String> dependenciesMap;
    private boolean hasDependency;

    public IGTemplateContext getIgTemplateContext() {
        return igTemplateContext;
    }

    public void setIgTemplateContext(IGTemplateContext igTemplateContext) {
        this.igTemplateContext = igTemplateContext;
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    public void setBasePackageName(String basePackageName) {
        this.basePackageName = basePackageName;
    }

    public Map<String, ResourceTemplateContext> getResourceTemplateContextMap() {
        return resourceTemplateContextMap;
    }

    public void setResourceTemplateContextMap(Map<String, ResourceTemplateContext> resourceTemplateContextMap) {
        this.resourceTemplateContextMap = resourceTemplateContextMap;
    }

    public Map<String, String> getResourceNameTypeMap() {
        return resourceNameTypeMap;
    }

    public void setResourceNameTypeMap(Map<String, String> resourceNameTypeMap) {
        this.resourceNameTypeMap = resourceNameTypeMap;
    }

    public Set<String> getDataTypesRegistry() {
        return dataTypesRegistry;
    }

    public void setDataTypesRegistry(Set<String> dataTypesRegistry) {
        this.dataTypesRegistry = dataTypesRegistry;
    }

    public Map<String, String> getDependenciesMap() {
        return dependenciesMap;
    }

    public void setDependenciesMap(Map<String, String> dependenciesMap) {
        if (!dependenciesMap.isEmpty())
            setHasDependency(true);
        this.dependenciesMap = dependenciesMap;
    }

    public boolean isHasDependency() {
        return hasDependency;
    }

    public void setHasDependency(boolean hasDependency) {
        this.hasDependency = hasDependency;
    }
}
