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

/**
 * Model for Ballerina Annotation Element
 */
public class AnnotationElement {
    private String name;
    private String dataType;
    private String min;
    private String max;
    private boolean isArray;
    private String description;
    private String path;
    private String valueSet;
    private boolean mustSupport;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
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
        if (max.equals("*")) {
            max = "int:MAX_VALUE";
        }
        this.max = max;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getValueSet() {
        return valueSet;
    }

    public void setValueSet(String valueSet) {
        this.valueSet = valueSet;
    }

    public boolean isMustSupport() {
        return mustSupport;
    }

    public void setMustSupport(boolean mustSupport) {
        this.mustSupport = mustSupport;
    }
}
