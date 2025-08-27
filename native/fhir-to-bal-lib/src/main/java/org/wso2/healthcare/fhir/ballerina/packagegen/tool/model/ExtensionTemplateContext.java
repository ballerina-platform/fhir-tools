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
    private Map<String, DatatypeTemplateContext> extensionDatatypes;
    private Map<String, Set<String>> extensionSlices;
    private Map<String, Set<String>> extensionResources;

    public ExtensionTemplateContext() {
        extensionDatatypes = new TreeMap<>();
        extensionSlices = new HashMap<>();
        extensionResources = new HashMap<>();
    }

    public Map<String, DatatypeTemplateContext> getExtensionDatatypes() {
        return extensionDatatypes;
    }

    public void setExtensionDatatypes(Map<String, DatatypeTemplateContext> extensionDatatypes) {
        this.extensionDatatypes = extensionDatatypes;
    }

    public Map<String, Set<String>> getExtensionSlices() {
        return extensionSlices;
    }

    public void setExtensionSlices(Map<String, Set<String>> extensionSlices) {
        this.extensionSlices = extensionSlices;
    }

    public Map<String, Set<String>> getExtensionResources() {
        return extensionResources;
    }

    public void setExtensionResources(Map<String, Set<String>> extensionResources) {
        this.extensionResources = extensionResources;
    }
}
