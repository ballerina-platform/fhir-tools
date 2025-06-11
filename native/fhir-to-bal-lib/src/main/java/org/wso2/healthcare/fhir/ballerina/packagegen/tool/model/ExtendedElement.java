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

/**
 * Model for extended FHIR element
 */
public class ExtendedElement {
    private String typeName;
    private BallerinaDataType balDataType;
    private String baseType;
    private String path;
    private DataTypeDefinitionAnnotation annotation;
    private HashMap<String, Element> elements;

    // Store datatype of extended elements extended from a primitive type
    // e.g: FHIR R5 EuropeBase --> patient.birthDate
    private String primitiveExtendedType;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public BallerinaDataType getBalDataType() {
        return balDataType;
    }

    public void setBalDataType(BallerinaDataType balDataType) {
        this.balDataType = balDataType;
    }

    public String getBaseType() {
        return baseType;
    }

    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public DataTypeDefinitionAnnotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(DataTypeDefinitionAnnotation annotation) {
        this.annotation = annotation;
    }

    public HashMap<String, Element> getElements() {
        return elements;
    }

    public void setElements(HashMap<String, Element> elements) {
        this.elements = elements;
    }

    @Override
    public boolean equals(Object o) {
        if (getClass() != o.getClass())
            return false;
        ExtendedElement otherElement = (ExtendedElement) o;
        if (this.balDataType.equals(otherElement.getBalDataType())) {
            if (this.elements != null && otherElement.elements != null) {
                return this.getElements().keySet().equals(otherElement.getElements().keySet());
            }
        }
        return false;
    }

    public String getPrimitiveExtendedType() {
        return primitiveExtendedType;
    }

    public void setPrimitiveExtendedType(String primitiveExtendedType) {
        this.primitiveExtendedType = primitiveExtendedType;
    }
}
