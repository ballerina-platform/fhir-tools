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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.model;

import java.util.Map;
import java.util.Set;

/**
 * Class holder for package related template context
 */
public class PackageTemplateContext {
    private IGTemplateContext igTemplateContext;
    private String basePackageName;
    private String internationalPackageName;
    private Map<String, ResourceTemplateContext> resourceTemplateContextMap;
    private Map<String, DatatypeTemplateContext> datatypeTemplateContextMap;
    private ExtensionTemplateContext extensionTemplateContext;
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

    public String getInternationalPackageName() {
        return internationalPackageName;
    }

    public void setInternationalPackageName(String internationalPackageName) {
        this.internationalPackageName = internationalPackageName;
    }

    public Map<String, ResourceTemplateContext> getResourceTemplateContextMap() {
        return resourceTemplateContextMap;
    }

    public void setResourceTemplateContextMap(Map<String, ResourceTemplateContext> resourceTemplateContextMap) {
        this.resourceTemplateContextMap = resourceTemplateContextMap;
    }

    public Map<String, DatatypeTemplateContext> getDatatypeTemplateContextMap() {
        return datatypeTemplateContextMap;
    }

    public void setDatatypeTemplateContextMap(Map<String, DatatypeTemplateContext> datatypeTemplateContextMap) {
        this.datatypeTemplateContextMap = datatypeTemplateContextMap;
    }

    public void addDatatypeTemplateContext(String datatypeName, DatatypeTemplateContext context) {
        this.datatypeTemplateContextMap.putIfAbsent(datatypeName, context);
    }

    public ExtensionTemplateContext getExtensionTemplateContext() {
        return extensionTemplateContext;
    }

    public void setExtensionTemplateContext(ExtensionTemplateContext extensionTemplateContext) {
        this.extensionTemplateContext = extensionTemplateContext;
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
