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
 * Model for FHIR resource element
 */
public class Element {
    private String dataType;
    private String name;
    private String rootElementName;
    private String min;
    private String max;
    private boolean isRequired;
    private boolean isArray;
    private String path;
    private String valueSet;
    private boolean hasChildElements;
    private HashMap<String, Element> childElements;
    private String description;

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public boolean isHasChildElements() {
        return hasChildElements;
    }

    public void setHasChildElements(boolean hasChildElements) {
        this.hasChildElements = hasChildElements;
    }

    public HashMap<String, Element> getChildElements() {
        return childElements;
    }

    public void setChildElements(HashMap<String, Element> childElements) {
        this.hasChildElements = true;
        this.childElements = childElements;
    }

    public String getRootElementName() {
        return rootElementName;
    }

    public void setRootElementName(String rootElementName) {
        this.rootElementName = rootElementName;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getValueSet() {
        return valueSet;
    }

    public void setValueSet(String valueSet) {
        this.valueSet = valueSet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
