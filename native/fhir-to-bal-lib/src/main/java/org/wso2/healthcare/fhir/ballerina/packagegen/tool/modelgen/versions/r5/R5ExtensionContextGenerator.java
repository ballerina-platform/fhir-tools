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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.versions.r5;

import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRDataTypeDef;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r5.model.FHIRR5DataTypeDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.DataTypesRegistry;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.AnnotationElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DataTypeDefinitionAnnotation;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.AbstractExtensionContextGenerator;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

public class R5ExtensionContextGenerator extends AbstractExtensionContextGenerator {
    public R5ExtensionContextGenerator(FHIRSpecificationData specificationData) {
        super(specificationData);
    }

    @Override
    public void populateBaseExtensionContext() {
        for (Map.Entry<String, FHIRDataTypeDef> extensionDefEntry : getExtensionDefnMap().entrySet()) {

            FHIRR5DataTypeDef r5ExtensionDefn = (FHIRR5DataTypeDef) extensionDefEntry.getValue();

            if ("Extension".equals(r5ExtensionDefn.getDefinition().getType())) {
                /// Extensions will also be treated as an extended data type.
                DatatypeTemplateContext context = new DatatypeTemplateContext();
                String typeName = CommonUtil.getSplitTokenAt(r5ExtensionDefn.getDefinition().getUrl(), "/", ToolConstants.TokenPosition.END);
                String elementIdentifier = GeneratorUtils.getInstance().getUniqueIdentifierFromId(typeName);
                context.setName(elementIdentifier);
                context.setBaseDataType("ExtensionExtension");

                DataTypeDefinitionAnnotation annotation = new DataTypeDefinitionAnnotation();
                annotation.setName(elementIdentifier);
                context.setAnnotation(annotation);

                for (ElementDefinition elementDefinition : r5ExtensionDefn.getDefinition().getSnapshot().getElement()) {
                    if (elementDefinition.getPath().contains(".") && !elementDefinition.getId().contains(":")) {
                        /// Parsing first level extensions
                        String elementName = elementDefinition.getPath().substring(
                                elementDefinition.getPath().lastIndexOf(".") + 1);

                        if (elementDefinition.getMin() != 0 && !elementDefinition.getMax().equals("0")) {
                            Element element = new Element();
                            element.setMax(GeneratorUtils.getMaxCardinality(elementDefinition.getMax()));
                            element.setMin(elementDefinition.getMin());
                            element.setArray(!"0".equals(elementDefinition.getBase().getMax()) && !"1".equals(elementDefinition.getBase().getMax()));

                            if ("url".equals(elementName) && elementDefinition.getFixed() != null) {
                                List<String> fixedValues = new ArrayList<>();
                                String value = elementDefinition.getFixed().toString();
                                value = value.replaceAll("UriType|\\[|\\]", "");
                                fixedValues.add(value);
                                element.setFixedValue(fixedValues);
                                element.setDataType(value);
                            }

                            String typeCode = elementDefinition.getType().get(0).getCode();
                            if (typeCode == null) {
                                // Give a type for PrimitiveType Extensions marked with "_"
                                // E.g.: type [{_code:{...}}]
                                typeCode = "Extension";
                            }

                            if (GeneratorUtils.getInstance().shouldReplacedByBalType(typeCode)) {
                                if (elementDefinition.getFixed() == null) {
                                    element.setDataType(GeneratorUtils.getInstance().resolveDataType(typeCode));
                                }
                            } else {
                                element.setDataType(typeCode);
                            }

                            if (elementName.endsWith("[x]")) {
                                typeCode = elementDefinition.getType().get(0).getCode();
                                element.setDataType(typeCode);
                                context.setBaseDataType(CommonUtil.toCamelCase(typeCode) + "Extension");
                            }

                            element.setName(GeneratorUtils.getInstance().resolveMultiDataTypeFieldNames(elementName, typeCode));
                            element.setDescription(CommonUtil.parseMultilineString(elementDefinition.getDefinition()));
                            element.setPath(elementDefinition.getPath());

                            //populate annotations
                            AnnotationElement annotationElement = GeneratorUtils.getInstance().populateAnnotationElement(element);
                            if (GeneratorUtils.getInstance().shouldReplacedByBalType(typeCode)) {
                                annotationElement.setDataType(GeneratorUtils.getInstance().resolveDataType(typeCode));
                            }
                            annotation.addElement(annotationElement);
                            context.addElement(element);
                        }
                    }
                }
                populateExtensionResourceMap(elementIdentifier, extensionDefEntry.getValue());
                getExtensionTemplateContext().getExtendedDatatypes().putIfAbsent(r5ExtensionDefn.getDefinition().getUrl(), context);
                DataTypesRegistry.getInstance().addDataType(context.getName());
            }
        }
    }

    @Override
    public void populateSliceExtensionContext() {
        for (Map.Entry<String, FHIRDataTypeDef> extensionDefEntry : getExtensionDefnMap().entrySet()) {
            FHIRR5DataTypeDef r5ExtensionDefn = (FHIRR5DataTypeDef) extensionDefEntry.getValue();
            String urlId = CommonUtil.getSplitTokenAt(r5ExtensionDefn.getDefinition().getUrl(), "/", ToolConstants.TokenPosition.END);
            String rootExtensionName = GeneratorUtils.getInstance().getUniqueIdentifierFromId(urlId);

            if ("Extension".equals(r5ExtensionDefn.getDefinition().getType())) {
                for (ElementDefinition elementDefinition : r5ExtensionDefn.getDefinition().getSnapshot().getElement()) {
                    if (elementDefinition.getId().contains(":")) {
                        if (elementDefinition.getSliceName() != null) {
                            String rootSliceName = elementDefinition.getSliceName();

                            String contextName = rootExtensionName + CommonUtil.toCamelCase(elementDefinition.getSliceName());
                            contextName = GeneratorUtils.getInstance().getUniqueIdentifierFromId(contextName);

                            DatatypeTemplateContext context = new DatatypeTemplateContext();
                            context.setName(contextName);
                            context.setBaseDataType(r5ExtensionDefn.getDefinition().getType() + "Extension");

                            DataTypeDefinitionAnnotation annotation = new DataTypeDefinitionAnnotation();
                            annotation.setName(contextName);
                            context.setAnnotation(annotation);

                            for (ElementDefinition sliceElementDefinition : r5ExtensionDefn.getDefinition().getSnapshot().getElement()) {
                                if (sliceElementDefinition.getId().contains(rootSliceName)) {
                                    String[] childSliceNames = sliceElementDefinition.getId().split("[:.]");
                                    String childSliceName = childSliceNames[childSliceNames.length - 1];

                                    if (rootSliceName.equals(childSliceName)) {
                                        continue;
                                    }

                                    Element element = new Element();
                                    element.setMax(GeneratorUtils.getMaxCardinality(sliceElementDefinition.getMax()));
                                    element.setMin(sliceElementDefinition.getMin());
                                    element.setArray(!"0".equals(sliceElementDefinition.getBase().getMax()) && !"1".equals(sliceElementDefinition.getBase().getMax()));

                                    /// Read the datatype of the element from the Type.Ref component
                                    /// TypeCode usually have only one element.
                                    String typeCode = elementDefinition.getType().get(0).getCode();
                                    if (typeCode == null) {
                                        // Special Case: TypeCode is null
                                        // Give a type for PrimitiveType Extensions marked with "_"
                                        // E.g.: type [{_code:{...}}]
                                        typeCode = "Extension";
                                    }

                                    if (GeneratorUtils.getInstance().shouldReplacedByBalType(typeCode)) {
                                        element.setDataType(GeneratorUtils.getInstance().resolveDataType(typeCode));
                                    } else {
                                        element.setDataType(typeCode);
                                    }

                                    if (childSliceName.endsWith("[x]")) {
                                        typeCode = sliceElementDefinition.getType().get(0).getCode();
                                        element.setDataType(typeCode);
                                        context.setBaseDataType(CommonUtil.toCamelCase(typeCode) + "Extension");
                                    }

                                    if ("url".equals(childSliceName)) {
                                        if (sliceElementDefinition.hasFixed()) {
                                            List<String> fixedValues = new ArrayList<>();
                                            String value = sliceElementDefinition.getFixed().toString().replaceAll("UriType|\\[|\\]", "");
                                            fixedValues.add(value);
                                            element.setFixedValue(fixedValues);
                                        }
                                    }

                                    element.setName(GeneratorUtils.getInstance().resolveMultiDataTypeFieldNames(childSliceName, typeCode));
                                    element.setDescription(CommonUtil.parseMultilineString(sliceElementDefinition.getDefinition()));
                                    element.setPath(rootSliceName + "." + childSliceName);

                                    //populate annotations
                                    AnnotationElement annotationElement = GeneratorUtils.getInstance().populateAnnotationElement(element);
                                    if (GeneratorUtils.getInstance().shouldReplacedByBalType(annotationElement.getDataType())) {
                                        annotationElement.setDataType(GeneratorUtils.getInstance().resolveDataType(annotationElement.getDataType()));
                                    }
                                    annotation.addElement(annotationElement);
                                    context.addElement(element);
                                }
                            }
                            getExtensionTemplateContext().getExtendedDatatypes().putIfAbsent(contextName.toLowerCase(), context);
                            DataTypesRegistry.getInstance().addDataType(context.getName());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void populateExtensionResourceMap(String identifier, FHIRDataTypeDef extensionDef) {
        Set<String> extensionContext = new HashSet<>();

        /// Retrieve context of extensions
        FHIRR5DataTypeDef r5ExtensionDefinition = (FHIRR5DataTypeDef) extensionDef;
        for (StructureDefinition.StructureDefinitionContextComponent contextComponent : r5ExtensionDefinition.getDefinition().getContext()) {
            extensionContext.add(contextComponent.getExpression());
        }
        getExtensionTemplateContext().getExtendedResources().putIfAbsent(identifier, extensionContext);
    }
}
