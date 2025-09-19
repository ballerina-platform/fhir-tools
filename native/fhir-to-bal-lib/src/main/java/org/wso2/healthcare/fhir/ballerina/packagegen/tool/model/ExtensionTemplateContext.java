/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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
import java.util.TreeMap;
import java.util.HashMap;

public class ExtensionTemplateContext {
    private Map<String, DatatypeTemplateContext> extendedDatatypes;
    private Map<String, Set<String>> extendedSlices;
    private Map<String, Set<String>> extendedResources;

    public ExtensionTemplateContext() {
        extendedDatatypes = new TreeMap<>();
        extendedSlices = new HashMap<>();
        extendedResources = new HashMap<>();
    }

    public Map<String, DatatypeTemplateContext> getExtendedDatatypes() {
        return extendedDatatypes;
    }

    public void setExtendedDatatypes(Map<String, DatatypeTemplateContext> extendedDatatypes) {
        this.extendedDatatypes = extendedDatatypes;
    }

    public Map<String, Set<String>> getExtendedSlices() {
        return extendedSlices;
    }

    public void setExtendedSlices(Map<String, Set<String>> extendedSlices) {
        this.extendedSlices = extendedSlices;
    }

    public Map<String, Set<String>> getExtendedResources() {
        return extendedResources;
    }

    public void setExtendedResources(Map<String, Set<String>> extendedResources) {
        this.extendedResources = extendedResources;
    }
}
