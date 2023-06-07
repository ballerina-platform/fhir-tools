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

/**
 * Model for extended FHIR element
 */
public class ExtendedElement {
    private String typeName;
    private BallerinaDataType balDataType;
    private DataTypeDefinitionAnnotation annotation;
    private HashMap<String, Element> elements;

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
}
