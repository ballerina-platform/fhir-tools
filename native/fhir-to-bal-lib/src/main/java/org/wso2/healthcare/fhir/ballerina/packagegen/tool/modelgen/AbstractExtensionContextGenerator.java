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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRDataTypeDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.DataTypesRegistry;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ExtensionTemplateContext;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

/**
 *
 * Abstract class for extensions generator context.
 * Extended by the version specific extension context generators.
 * Define abstract methods to be implemented by the child classes in extension generation.
 */
public abstract class AbstractExtensionContextGenerator {
    private static final Log LOG = LogFactory.getLog(AbstractExtensionContextGenerator.class);
    private final Map<String, FHIRDataTypeDef> extensionDefnMap;
    private final ExtensionTemplateContext extensionTemplateContext;

    public AbstractExtensionContextGenerator(FHIRSpecificationData fhirSpecificationData) {
        LOG.info("Started: Extensions Generation");

        this.extensionDefnMap = fhirSpecificationData.getDataTypes();
        this.extensionTemplateContext = new ExtensionTemplateContext();

        populateExtensionTemplateContext();

        LOG.info("Ended: Extensions Generation");
    }

    protected Map<String, FHIRDataTypeDef> getExtensionDefnMap() {
        return extensionDefnMap;
    }

    public ExtensionTemplateContext getExtensionTemplateContext() {
        return extensionTemplateContext;
    }

    private void populateExtensionTemplateContext() {
        populateBaseExtensionContext();
        populateSliceExtensionContext();
        populateExtensionSliceMap();
    }

    protected abstract void populateBaseExtensionContext();

    protected abstract void populateSliceExtensionContext();

    protected abstract void populateExtensionResourceMap(String identifier, FHIRDataTypeDef extensionDef);

    protected void populateExtensionSliceMap() {
        Map<String, Set<String>> childExtensionMap = new HashMap<>();
        Set<String> searchKeys = new HashSet<>();
        Map<String, DatatypeTemplateContext> extensionDatatypeMap = extensionTemplateContext.getExtendedDatatypes();

        for (Map.Entry<String, DatatypeTemplateContext> contextEntry : extensionDatatypeMap.entrySet()) {
            if (contextEntry.getKey().contains("http://")) {
                /// Set Base Extension
                searchKeys.add(contextEntry.getValue().getName());
                String extensionArrName = contextEntry.getValue().getName() + "Extensions";
                childExtensionMap.putIfAbsent(extensionArrName, new HashSet<>());
                DataTypesRegistry.getInstance().addDataType(extensionArrName);
            }
        }

        for (Map.Entry<String, DatatypeTemplateContext> contextEntry : extensionDatatypeMap.entrySet()) {
            /// Add Slice Extensions
            /// Ensure that http:// keys are processed first
            for (String key : searchKeys) {
                if (contextEntry.getKey().contains(key.toLowerCase())) {
                    childExtensionMap.get(key + "Extensions").add(contextEntry.getValue().getName());
                }
            }

        }

        childExtensionMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        extensionTemplateContext.setExtendedSlices(childExtensionMap);
    }
}
