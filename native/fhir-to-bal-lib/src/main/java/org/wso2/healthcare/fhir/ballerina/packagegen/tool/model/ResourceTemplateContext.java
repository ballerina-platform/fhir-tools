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
