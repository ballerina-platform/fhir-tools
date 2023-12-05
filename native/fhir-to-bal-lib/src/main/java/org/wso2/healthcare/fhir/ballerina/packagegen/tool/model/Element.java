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

import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model for FHIR resource element
 */
public class Element {
    private String dataType;
    private Map<String, String> profiles;
    private String name;
    private String path;
    private int min;
    private int max;
    private List<String> fixedValue;
    private String valueSet;
    private HashMap<String, Element> childElements;
    private boolean isArray;
    private boolean isSlice;
    private boolean isExtended;
    private String rootElementName;
    private String description;
    private String summary;
    private String requirement;

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    public Map<String, String> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<String, String> profiles) {
        this.profiles = profiles;
    }

    public void addProfile(String profile, String dataType) {
        if (this.profiles == null) {
            this.profiles = new HashMap<>();
        }
        this.profiles.putIfAbsent(profile, dataType);
    }

    public String getTypeWithImportPrefix() {
        return isExtended ? this.dataType : GeneratorUtils.getInstance().getTypeWithImport(this.dataType);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasFixedValue() {
        return (fixedValue != null && !fixedValue.isEmpty());
    }

    public List<String> getFixedValue() {
        return fixedValue;
    }

    public void setFixedValue(List<String> fixedValue) {
        this.fixedValue = fixedValue;
    }

    public boolean isRequired() {
        return this.min > 0 || this.hasFixedValue();
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

    public void setSlice(boolean slice) {
        isSlice = slice;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public boolean isSlice() {
        return isSlice;
    }

    public void setIsSlice(boolean isASlice) {
        this.isSlice = isASlice;
    }

    public boolean isExtended() {
        return isExtended;
    }

    public void setExtended(boolean extended) {
        isExtended = extended;
    }

    public boolean hasChildElements() {
        return this.childElements != null && !this.childElements.isEmpty();
    }

    public HashMap<String, Element> getChildElements() {
        return childElements;
    }

    public void setChildElements(HashMap<String, Element> childElements) {
        this.childElements = childElements;
    }

    public String getRootElementName() {
        return rootElementName;
    }

    public void setRootElementName(String rootElementName) {
        this.rootElementName = rootElementName;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }
}
