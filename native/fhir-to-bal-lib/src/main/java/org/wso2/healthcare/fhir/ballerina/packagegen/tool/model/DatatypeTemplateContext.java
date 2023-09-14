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
import java.util.Map;

/**
 * Template context information holder for FHIR datatype definitions.
 */
public class DatatypeTemplateContext {
    private String name;
    private String baseDataType;
    private String profile;
    private final Map<String, Element> elements = new HashMap<>();
    private HashMap<String, ExtendedElement> extendedElements = new HashMap<>();
    private DataTypeDefinitionAnnotation annotation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addElement(Element element) {
        elements.putIfAbsent(element.getName(), element);
    }

    public Map<String, Element> getElements() {
        return elements;
    }

    public String getBaseDataType() {
        return baseDataType;
    }

    public void setBaseDataType(String baseDataType) {
        this.baseDataType = baseDataType;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public DataTypeDefinitionAnnotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(DataTypeDefinitionAnnotation annotation) {
        this.annotation = annotation;
    }

    public HashMap<String, ExtendedElement> getExtendedElements() {
        return extendedElements;
    }

    public void setExtendedElements(HashMap<String, ExtendedElement> extendedElements) {
        this.extendedElements = extendedElements;
    }
}
