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
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.BallerinaDataType;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ExtendedElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class for generating context for FHIR data types.
 * This class provides methods to populate and manage the context for FHIR data types.
 * Extended by R4DatatypeContextGenerator and R5DatatypeContextGenerator
 */

public abstract class AbstractDatatypeContextGenerator {
    private static final Log LOG = LogFactory.getLog(AbstractDatatypeContextGenerator.class);
    private final Map<String, FHIRDataTypeDef> datatypeDefnMap;
    private final Map<String, DatatypeTemplateContext> dataTypeTemplateContextMap;

    public AbstractDatatypeContextGenerator(FHIRSpecificationData fhirSpecificationData) {
        this.datatypeDefnMap = fhirSpecificationData.getDataTypes();
        this.dataTypeTemplateContextMap = new HashMap<>();
        populateDatatypeContext();
    }

    protected Map<String, FHIRDataTypeDef> getDataTypeDefnMap() {
        return datatypeDefnMap;
    }

    public Map<String, DatatypeTemplateContext> getDatatypeTemplateContextMap() {
        return dataTypeTemplateContextMap;
    }

    protected void populateExtendedElementsMap(Element element, DatatypeTemplateContext context) {
        LOG.debug("Started: Resource Extended Element Map population");
        if (!element.getDataType().equals("Extension")) {
            if (element.hasChildElements()) {
                for (Map.Entry<String, Element> childEntry : element.getChildElements().entrySet()) {
                    populateExtendedElementsMap(childEntry.getValue(), context);
                }
            }
            ExtendedElement extendedElement;
            String elementDataType = element.getDataType();
            if (elementDataType.equals("code") && element.hasChildElements()) {
                extendedElement = GeneratorUtils.getInstance().populateExtendedElement(element, BallerinaDataType.Enum, elementDataType,
                        context.getName());
                context.getExtendedElements().putIfAbsent(element.getName(), extendedElement);
                element.setExtended(true);
            }
        }
        LOG.debug("Ended: Resource Extended Element Map population");
    }

    protected abstract void populateDatatypeContext();
}
