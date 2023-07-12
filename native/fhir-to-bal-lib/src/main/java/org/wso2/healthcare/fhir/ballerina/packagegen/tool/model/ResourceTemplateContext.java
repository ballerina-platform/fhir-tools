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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Class holder for resource related template context
 */
public class ResourceTemplateContext {

    private String resourceType;
    private String resourceName;
    private String profile;
    private String igName;
    private String baseIgName;
    private ResourceDefinitionAnnotation resourceDefinitionAnnotation;
    private HashMap<String, Element> elements;
    private HashMap<String, ExtendedElement> ExtendedElements;
    private Set<String> resourceDependencies = new HashSet<>();

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getIgName() {
        return igName;
    }

    public void setIgName(String igName) {
        this.igName = igName;
    }

    public String getBaseIgName() {
        return baseIgName;
    }

    public void setBaseIgName(String baseIgName) {
        this.baseIgName = baseIgName;
    }

    public ResourceDefinitionAnnotation getResourceDefinitionAnnotation() {
        return resourceDefinitionAnnotation;
    }

    public void setResourceDefinitionAnnotation(ResourceDefinitionAnnotation resourceDefinitionAnnotation) {
        this.resourceDefinitionAnnotation = resourceDefinitionAnnotation;
    }

    public HashMap<String, Element> getElements() {
        return elements;
    }

    public void setElements(HashMap<String, Element> elements) {
        this.elements = elements;
    }

    public HashMap<String, ExtendedElement> getExtendedElements() {
        return ExtendedElements;
    }

    public void setExtendedElements(HashMap<String, ExtendedElement> extendedElements) {
        ExtendedElements = extendedElements;
    }

    public Set<String> getResourceDependencies() {
        return resourceDependencies;
    }

    public void setResourceDependencies(Set<String> resourceDependencies) {
        this.resourceDependencies = resourceDependencies;
    }
}
