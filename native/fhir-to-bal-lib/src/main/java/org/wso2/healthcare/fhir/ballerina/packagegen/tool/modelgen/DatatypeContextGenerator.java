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
package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRDataTypeDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.AnnotationElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.BallerinaDataType;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DataTypeDefinitionAnnotation;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ExtendedElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Template context generator for FHIR datatypes to be used to generate datatypes source file.
 */
public class DatatypeContextGenerator {

    private static final Log LOG = LogFactory.getLog(DatatypeContextGenerator.class);
    private final Map<String, FHIRDataTypeDef> datatypeDefnMap;
    private final Map<String, DatatypeTemplateContext> dataTypeTemplateContextMap;
    private static final List<String> DEFAULT_DATA_TYPES = Arrays.asList("Ratio", "Period", "Range", "Attachment",
            "Identifier", "Annotation", "HumanName", "CodeableConcept", "ContactPoint", "Coding", "Money", "Address",
            "Timing", "BackboneElement", "Quantity", "SampledData", "Signature", "Age", "Distance", "Duration", "Count",
            "MoneyQuantity", "SimpleQuantity", "ContactDetail", "Contributor", "DataRequirement", "RelatedArtifact",
            "Element", "UsageContext", "ParameterDefinition", "Expression", "TriggerDefinition", "Reference", "Meta",
            "Dosage", "xhtml", "ElementDefinition", "Extension", "Narrative", "ProdCharacteristic", "Population", "SubstanceAmount"
    );

    public DatatypeContextGenerator(FHIRSpecificationData fhirSpecificationData) {
        this.datatypeDefnMap = fhirSpecificationData.getDataTypes();
        this.dataTypeTemplateContextMap = new HashMap<>();
        populateDatatypeContext();
    }

    public Map<String, FHIRDataTypeDef> getDatatypeDefnMap() {
        return datatypeDefnMap;
    }

    public Map<String, DatatypeTemplateContext> getDataTypeTemplateContextMap() {
        return dataTypeTemplateContextMap;
    }

    private void populateDatatypeContext() {
        for (Map.Entry<String, FHIRDataTypeDef> datatypeDefnEntry : datatypeDefnMap.entrySet()) {
            FHIRDataTypeDef datatypeDefn = datatypeDefnEntry.getValue();
            if (DEFAULT_DATA_TYPES.contains(datatypeDefn.getDefinition().getName())
                    || "Extension".equals(datatypeDefn.getDefinition().getType())) {
                continue;
            }
            DatatypeTemplateContext context = new DatatypeTemplateContext();
            String typeName = CommonUtil.getSplitTokenAt(datatypeDefn.getDefinition().getUrl(), "/", ToolConstants.TokenPosition.END);
            context.setName(GeneratorUtils.getInstance().getUniqueIdentifierFromId(typeName));
            context.setBaseDataType(datatypeDefn.getDefinition().getType());
            DataTypeDefinitionAnnotation annotation = new DataTypeDefinitionAnnotation();
            annotation.setName(datatypeDefn.getDefinition().getName());
            context.setAnnotation(annotation);
            for (ElementDefinition elementDefinition : datatypeDefn.getDefinition().getSnapshot().getElement()) {
                if (elementDefinition.getPath().contains(".")) {
                    String elementName = elementDefinition.getPath().substring(
                            elementDefinition.getPath().lastIndexOf(".") + 1);
                    if ("id".equals(elementName) || "extension".equals(elementName)) {
                        continue;
                    }
                    Element element = new Element();
                    element.setMax(GeneratorUtils.getMaxCardinality(elementDefinition.getMax()));
                    element.setMin(elementDefinition.getMin());
                    element.setArray(!elementDefinition.getBase().getMax().equals("0") && !elementDefinition.getBase().getMax().equals("1"));
                    String typeCode = elementDefinition.getType().get(0).getCode();
                    if (GeneratorUtils.getInstance().shouldReplacedByBalType(typeCode)) {
                        element.setDataType(GeneratorUtils.getInstance().resolveDataType(typeCode));
                    } else if (ToolConstants.ELEMENT.equals(typeCode)) {
                        element.setDataType(ToolConstants.ELEMENT + CommonUtil.toCamelCase(elementName));
                    } else {
                        element.setDataType(typeCode);
                    }
                    if (elementName.endsWith("[x]") && datatypeDefn.getDefinition().getType().equals(
                            ToolConstants.DATA_TYPE_EXTENSION)) {
                        context.setBaseDataType(StringUtils.capitalize(typeCode + ToolConstants.DATA_TYPE_EXTENSION));
                    } else if ("code".equals(typeCode)) {
                        GeneratorUtils.populateCodeValuesForCodeElements(elementDefinition, element);
                    }
                    element.setName(GeneratorUtils.getInstance().resolveMultiDataTypeFieldNames(elementName, typeCode));
                    element.setDescription(CommonUtil.parseMultilineString(elementDefinition.getDefinition()));
                    element.setPath(elementDefinition.getPath());
                    //populate extended elements
                    populateExtendedElementsMap(element, context);
                    //populate annotations
                    AnnotationElement annotationElement = GeneratorUtils.getInstance().populateAnnotationElement(element);
                    annotation.addElement(annotationElement);
                    context.addElement(element);
                }
            }
            dataTypeTemplateContextMap.putIfAbsent(datatypeDefn.getDefinition().getUrl(), context);
        }
    }

    private void populateExtendedElementsMap(Element element, DatatypeTemplateContext context) {
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
                context.getExtendedElements().putIfAbsent(extendedElement.getTypeName(), extendedElement);
                element.setName(extendedElement.getTypeName());
                element.setExtended(true);
            }
        }
        LOG.debug("Ended: Resource Extended Element Map population");
    }
}
