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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Base;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRResourceDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.AnnotationElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.BallerinaDataType;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DataTypeDefinitionAnnotation;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ExtendedElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ResourceDefinitionAnnotation;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ResourceTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Comparator;
import java.util.regex.Pattern;

import static org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants.CONSTRAINTS_LIB_IMPORT;

/**
 * Generator class for resource related context
 */
public class ResourceContextGenerator {
    private static final Log LOG = LogFactory.getLog(ResourceContextGenerator.class);
    public final Set<String> baseResources = new HashSet<>(Arrays.asList("Bundle", "OperationOutcome", "CodeSystem", "ValueSet"));
    private final BallerinaPackageGenToolConfig toolConfig;
    private ResourceTemplateContext resourceTemplateContextInstance;
    private final Map<String, ResourceTemplateContext> resourceTemplateContextMap;
    private final Map<String, String> resourceNameTypeMap;
    private final Set<String> dataTypesRegistry;

    public ResourceContextGenerator(BallerinaPackageGenToolConfig config, FHIRImplementationGuide ig,
                                    Set<String> dataTypesRegistry) {
        LOG.debug("Resource Context Generator Initiated");
        this.toolConfig = config;
        this.resourceTemplateContextMap = new HashMap<>();
        this.resourceNameTypeMap = new HashMap<>();
        this.dataTypesRegistry = dataTypesRegistry;
        populateResourceTemplateContexts(ig);
    }

    /**
     * Populate resource template contexts
     *
     * @param ig FHIR implementation guide DTO
     */
    private void populateResourceTemplateContexts(FHIRImplementationGuide ig) {
        LOG.debug("Started: Resource Template Context population");
        for (Map.Entry<String, FHIRResourceDef> definitionEntry : ig.getResources().entrySet()) {
            StructureDefinition structureDefinition = definitionEntry.getValue().getDefinition();
            if (!baseResources.contains(structureDefinition.getType())) {
                this.resourceNameTypeMap.put(structureDefinition.getName(), structureDefinition.getType());

                this.resourceTemplateContextInstance = new ResourceTemplateContext();
                this.resourceTemplateContextInstance.setResourceType(structureDefinition.getType());
                this.resourceTemplateContextInstance.setResourceName(structureDefinition.getName());
                this.resourceTemplateContextInstance.setProfile(definitionEntry.getValue().getDefinition().getUrl());
                this.resourceTemplateContextInstance.setIgName(ig.getName());

                ResourceDefinitionAnnotation resourceDefinitionAnnotation = new ResourceDefinitionAnnotation();
                resourceDefinitionAnnotation.setName(structureDefinition.getName());
                resourceDefinitionAnnotation.setBaseType(CommonUtil.getSplitTokenAt(structureDefinition
                        .getBaseDefinition(), File.separator, ToolConstants.TokenPosition.END));
                resourceDefinitionAnnotation.setProfile(this.resourceTemplateContextInstance.getProfile());
                resourceDefinitionAnnotation.setElements(new HashMap<>());
                this.resourceTemplateContextInstance.setResourceDefinitionAnnotation(resourceDefinitionAnnotation);

                populateSnapshotElementMap(structureDefinition.getSnapshot().getElement());

                for (Element snapshotElement : this.resourceTemplateContextInstance.getSnapshotElements().values()) {
                    markExtendedElements(snapshotElement);
                    populateResourceSliceElementsMap(snapshotElement);
                    populateResourceElementMap(snapshotElement);
                }

                for (Element resourceElement : this.resourceTemplateContextInstance.getResourceElements().values()) {
                    populateResourceExtendedElementsMap(resourceElement);
                    populateResourceElementAnnotationsMap(resourceElement);
                }

                for (List<Element> slices : this.resourceTemplateContextInstance.getSliceElements().values()) {
                    for (Element slice : slices) {
                        populateResourceExtendedElementsMap(slice);
                    }
                }

                this.resourceTemplateContextMap.put(structureDefinition.getName(), this.resourceTemplateContextInstance);
            }
        }
        LOG.debug("Ended: Resource Template Context population");
    }

    /**
     * Populate resource elements map in a hierarchical way
     *
     * @param elementDefinitions      FHIR element definition DTO
     */
    private void populateSnapshotElementMap(List<ElementDefinition> elementDefinitions) {
        LOG.debug("Started: Snapshot Element Map population");
        elementDefinitions.sort(new Comparator<ElementDefinition>() {
            @Override
            public int compare(ElementDefinition e1, ElementDefinition e2) {
                return e1.getPath().compareToIgnoreCase(e2.getPath());
            }
        });

        String elementPath;
        String[] elementPathTokens;
        HashMap<String, Element> snapshotElementMap = new HashMap<>();

        boolean isSlice;
        String sliceNamePattern;
        for (ElementDefinition elementDefinition : elementDefinitions) {
            isSlice = false;
            elementPath = elementDefinition.getPath();

            // Adding logic to handle multi datatype element definitions in the
            // format of <RESOURCE>.<field>[x]:<field><Datatype>.<childPath>
            // i.e : MedicationRequest.medication[x]:medicationCodeableConcept.coding
            String id = elementDefinition.getId();
            if (id.contains("[x]:")) {
                elementPath = id.replaceAll("\\w+([A-Z]?\\[x]:)", "");
                if (elementPath.contains(":")) {
                    elementPath = elementPath.replaceAll("\\w+([A-Z]?:)", "");
                    String[] pathTokens = elementPath.split("\\.");
                    sliceNamePattern = ":" + pathTokens[pathTokens.length - 1];
                    if (id.contains(sliceNamePattern)) {
                        isSlice = true;
                    }
                }
            } else if (id.contains(":")) {
                elementPath = id.replaceAll("\\w+([A-Z]?:)", "");
                String[] pathTokens = elementPath.split("\\.");
                sliceNamePattern = ":" + pathTokens[pathTokens.length - 1];
                if (id.contains(sliceNamePattern)) {
                    isSlice = true;
                }
            }
            elementPathTokens = elementPath.split("\\.");

            if (elementPathTokens.length > 1) {
                String rootElementName;
                String elementName;
                String resourceName = elementPathTokens[0];
                elementPath = elementPath.substring(resourceName.length() + 1);

                if (elementPath.split("\\.").length > 1) {
                    Map<String, Element> elementMap = snapshotElementMap;
                    while (elementPath.split("\\.").length > 1) {
                        elementPathTokens = elementPath.split("\\.");
                        rootElementName = elementPathTokens[0];

                        if (elementPathTokens.length == 2) {
                            elementName = elementPathTokens[1];
                            if (elementMap.containsKey(rootElementName)) {
                                Element rootElement = elementMap.get(rootElementName);
                                List<ElementDefinition.TypeRefComponent> types = elementDefinition.getType();
                                String tempElement = elementName.split(Pattern.quote("[x]"))[0];
                                for (ElementDefinition.TypeRefComponent type : elementDefinition.getType()) {
                                    if (types.size() > 1 || elementName.contains("[x]"))
                                        elementName = tempElement + CommonUtil.toCamelCase(type.getCode());

                                    String dataType = type.getCode();
                                    Element childElement = populateElement(rootElementName, elementName, dataType, isSlice, elementDefinition);
                                    if (rootElement.getChildElements() != null) {
                                        if (!rootElement.getChildElements().containsKey(elementName)) {
                                            rootElement.getChildElements().put(elementName, childElement);
                                        }
                                    } else {
                                        HashMap<String, Element> newChildElements = new HashMap<>();
                                        newChildElements.put(elementName, childElement);
                                        rootElement.setChildElements(newChildElements);
                                    }
                                }
                            }
                        }
                        elementPath = elementPath.substring(rootElementName.length() + 1);
                        if (elementMap.containsKey(rootElementName) && elementMap.get(rootElementName).hasChildElements())
                            elementMap = elementMap.get(rootElementName).getChildElements();
                    }
                } else {
                    List<ElementDefinition.TypeRefComponent> types = elementDefinition.getType();
                    String tempElement = elementPath.split(Pattern.quote("[x]"))[0];
                    for (ElementDefinition.TypeRefComponent type : elementDefinition.getType()) {
                        if (types.size() > 1 || elementPath.contains("[x]"))
                            elementPath = tempElement + CommonUtil.toCamelCase(type.getCode());
                        Element element = populateElement(resourceName, elementPath, type.getCode(), isSlice, elementDefinition);
                        snapshotElementMap.put(elementPath, element);
                    }
                }
            }
        }
        this.resourceTemplateContextInstance.setSnapshotElements(snapshotElementMap);
        LOG.debug("Ended: Snapshot Element Map population");
    }

    /**
     * Create resource element
     *
     * @param rootName          element root
     * @param name              element name
     * @param type              element data type
     * @param elementDefinition element definition DTO
     * @return created element object
     */
    private Element populateElement(String rootName, String name, String type, boolean isSlice, ElementDefinition elementDefinition) {
        LOG.debug("Started: Resource Element population");
        Element element = new Element();
        element.setName(name);
        element.setRootElementName(rootName);
        element.setDataType(GeneratorUtils.resolveDataType(toolConfig, type));

        if (elementDefinition.hasFixed()) {
            ArrayList<String> values = new ArrayList<>();
            values.add(elementDefinition.getFixed().primitiveValue());
            element.setFixedValue(values);
        } else if(elementDefinition.hasPattern() && !elementDefinition.getPattern().children().isEmpty()) {
            HashMap<String, Element> childElements = new HashMap<>();
            for (Property childProperty : elementDefinition.getPattern().children()) {
                if (childProperty.hasValues()) {
                    Element childElement = populateChildElementProperties(childProperty, elementDefinition.getPath());
                    childElements.put(childProperty.getName(), childElement);
                }
            }
            element.setChildElements(childElements);
        }

        element.setMin(String.valueOf(elementDefinition.getMin()));
        element.setMax(elementDefinition.getMax());
        element.setArray(isElementArray(elementDefinition));
        element.setIsSlice(isSlice);
        element.setPath(elementDefinition.getPath());
        element.setRequired(elementDefinition.getMin() > 0);
        element.setValueSet(elementDefinition.getBinding().getValueSet());
        element.setDescription(CommonUtil.parseMultilineString(elementDefinition.getDefinition()));
        element.setSummary(CommonUtil.parseMultilineString(elementDefinition.getShort()));
        element.setRequirement(CommonUtil.parseMultilineString(elementDefinition.getRequirements()));

        /**
         Todo: Fix resolving of Codes from implementation Guide
         Refer Issue: https://github.com/wso2-enterprise/open-healthcare/issues/928
         **/
        if (element.getDataType().equals("code") && isCodedString(elementDefinition.getShort())) {
            HashMap<String, Element> childElements = new HashMap<>();
            String[] codes = elementDefinition.getShort().split(Pattern.quote("|"));
            for (String code : codes) {
                code = CommonUtil.validateCode(code);
                if (!code.trim().isEmpty()) {
                    Element childElement = new Element();
                    childElement.setName(code);
                    childElement.setDataType("string");
                    childElement.setRootElementName(element.getName());
                    childElement.setValueSet(element.getValueSet());
                    childElements.put(childElement.getName(), childElement);
                }
            }
            element.setChildElements(childElements);
        }
        markConstrainedElements(element);
        LOG.debug("Ended: Resource Element population");
        return element;
    }

    private Element populateChildElementProperties(Property childProperty, String elementPath) {
        Element childElement = new Element();
        childElement.setName(childProperty.getName());
        childElement.setDataType(childProperty.getTypeCode());
        childElement.setMin(String.valueOf(childProperty.getMinCardinality()));
        childElement.setMax(String.valueOf(childProperty.getMaxCardinality()));
        childElement.setArray(childProperty.isList());
        childElement.setMin("1");
        if (childProperty.getMaxCardinality() == Integer.MAX_VALUE) {
            childElement.setMax("*");
        } else {
            childElement.setMax(String.valueOf(childProperty.getMaxCardinality()));
        }
        childElement.setDescription(childProperty.getDefinition());
        childElement.setPath(elementPath + "." + childProperty.getName());

        ArrayList<String> values = new ArrayList<>();
        for (Base value : childProperty.getValues()) {
            if (!value.hasPrimitiveValue()) {
                HashMap<String, Element> childElements = new HashMap<>();
                for (Property property : value.children()) {
                    if (property.hasValues())
                        childElements.put(property.getName(), populateChildElementProperties(property, childElement.getPath()));
                }
                childElement.setChildElements(childElements);
            } else {
                values.add(value.primitiveValue());
            }
        }
        childElement.setFixedValue(values);
        markConstrainedElements(childElement);
        return childElement;
    }

    private void populateResourceElementMap(Element element) {
        if (!element.isSlice()) {
            if (element.hasChildElements()) {
                Iterator<Map.Entry<String, Element>> rootIterator = element.getChildElements().entrySet().iterator();
                Iterator<Map.Entry<String, Element>> iterator = rootIterator;
                while (iterator.hasNext()) {
                    Map.Entry<String, Element> childEntry = iterator.next();
                    if (childEntry.getValue().isSlice()) {
                        iterator.remove();
                    } else if (childEntry.getValue().hasChildElements()) {
                        rootIterator = iterator;
                        iterator = childEntry.getValue().getChildElements().entrySet().iterator();
                    } else {
                        iterator = rootIterator;
                    }
                }
            }
            this.resourceTemplateContextInstance.getResourceElements().put(element.getName(), element);
        }
    }

    private void markConstrainedElements(Element element) {
        boolean isCardinalityConstrained = (Integer.parseInt(element.getMin()) >= 1) && ("*".equals(element.getMax()));
        boolean isConstraintsImportExists = this.resourceTemplateContextInstance.getResourceDependencies()
                .stream()
                .anyMatch(d -> d.equals(CONSTRAINTS_LIB_IMPORT));

        if (!isConstraintsImportExists && isCardinalityConstrained) {
            this.resourceTemplateContextInstance.getResourceDependencies().add(CONSTRAINTS_LIB_IMPORT);
        }
    }

    private void markExtendedElements(Element element) {
        if (!"Extension".equals(element.getDataType())) {
            if ("Meta".equals(element.getDataType()) || "Code".equals(element.getDataType()) || "BackboneElement".equals(element.getDataType()) || element.hasFixedValue()) {
                element.setExtended(true);
            }
            if (element.hasChildElements()) {
                for (Map.Entry<String, Element> childEntry : element.getChildElements().entrySet()) {
                    markExtendedElements(childEntry.getValue());
                    if (childEntry.getValue().isExtended()) {
                        element.setExtended(true);
                    }
                }
            }
        }
    }

    /**
     * Populate extended elements map
     *
     * @param element resource element
     */
    private void populateResourceExtendedElementsMap(Element element) {
        LOG.debug("Started: Resource Extended Element Map population");
        if (!element.getDataType().equals("Extension")) {
            if (element.hasChildElements()) {
                for (Map.Entry<String, Element> childEntry : element.getChildElements().entrySet()) {
                    populateResourceExtendedElementsMap(childEntry.getValue());
                }
            }
            validateAndPopulateExtendedElement(element);
        }
        LOG.debug("Ended: Resource Extended Element Map population");
    }

    /**
     * Validate and create extended elements from resource elements
     *
     * @param element            resource element to be validated
     */
    private void validateAndPopulateExtendedElement(Element element) {
        LOG.debug("Started: Resource Extended Element validation");
        ExtendedElement extendedElement;
        String elementDataType = element.getDataType();
        if (elementDataType.equals("code") && element.hasChildElements()) {
            extendedElement = populateExtendedElement(element, BallerinaDataType.Enum, elementDataType);
            putExtendedElementIfAbsent(element, extendedElement);
        } else if (element.isSlice() || elementDataType.equals("BackboneElement") || (element.isExtended() && element.hasChildElements())) {
            extendedElement = populateExtendedElement(element, BallerinaDataType.Record, elementDataType);
            extendedElement.setElements(element.getChildElements());

            DataTypeDefinitionAnnotation annotation = new DataTypeDefinitionAnnotation();
            annotation.setName(extendedElement.getTypeName());

            if (element.hasChildElements()) {
                HashMap<String, AnnotationElement> childElementAnnotations = new HashMap<>();
                for (Element subElement : element.getChildElements().values()) {
                    AnnotationElement annotationElement = populateAnnotationElement(subElement);
                    childElementAnnotations.put(annotationElement.getName(), annotationElement);
                }
                annotation.setElements(childElementAnnotations);
            }
            extendedElement.setAnnotation(annotation);

            putExtendedElementIfAbsent(element, extendedElement);
        }
        LOG.debug("Ended: Resource Extended Element validation");
    }

    /**
     * Create extended element object
     *
     * @param element resource element
     * @param balType Preferred Ballerina data type for the extended element
     * @return created extended element object
     */
    private ExtendedElement populateExtendedElement(Element element, BallerinaDataType balType, String baseType) {
        LOG.debug("Started: Resource Extended Element population");
        ExtendedElement extendedElement = new ExtendedElement();
        String extendedElementTypeName = generateExtendedElementIdentifier(element);
        extendedElement.setTypeName(extendedElementTypeName);
        element.setDataType(extendedElementTypeName);
        extendedElement.setBalDataType(balType);
        extendedElement.setBaseType(baseType);

        if (element.getChildElements() != null)
            extendedElement.setElements(element.getChildElements());

        LOG.debug("Ended: Resource Extended Element population");

        return extendedElement;
    }

    private void putExtendedElementIfAbsent(Element element, ExtendedElement extendedElement) {
        if (extendedElement != null) {
            boolean isAlreadyExists = this.resourceTemplateContextInstance.getExtendedElements().containsKey(extendedElement.getTypeName());
            if (isAlreadyExists) {
                element.setDataType(this.resourceTemplateContextInstance.getExtendedElements().get(extendedElement.getTypeName()).getTypeName());
            } else {
                this.resourceTemplateContextInstance.getExtendedElements().put(extendedElement.getTypeName(), extendedElement);
            }
        }
    }

    private void populateResourceSliceElementsMap(Element element) {
        LOG.debug("Started: Resource Slice Element Map population");
        if (element.hasChildElements()) {
            for (Map.Entry<String, Element> childEntry : element.getChildElements().entrySet()) {
                populateResourceSliceElementsMap(childEntry.getValue());
            }
        }

        if (element.isSlice()) {
            if(this.resourceTemplateContextInstance.getSliceElements().get(element.getPath()) != null) {
                this.resourceTemplateContextInstance.getSliceElements().get(element.getPath()).add(element);
            } else {
                ArrayList<Element> slices = new ArrayList<>();
                slices.add(element);
                this.resourceTemplateContextInstance.getSliceElements().put(element.getPath(), slices);
            }
        }
        LOG.debug("Ended: Resource Slice Element Map population");
    }

    private void populateResourceElementAnnotationsMap(Element element) {
        LOG.debug("Started: Resource Element Annotation Map population");
        AnnotationElement annotationElement = populateAnnotationElement(element);
        this.resourceTemplateContextInstance.getResourceDefinitionAnnotation().getElements().put(element.getName(), annotationElement);
        this.resourceTemplateContextInstance.getResourceDefinitionAnnotation().getElements().put(annotationElement.getName(), annotationElement);
        LOG.debug("Ended: Resource Element Annotation Map population");
    }

    /**
     * Create annotation element object
     *
     * @param element resource element
     * @return annotation element object
     */
    private AnnotationElement populateAnnotationElement(Element element) {
        LOG.debug("Started: Annotation Element population");
        AnnotationElement annotationElement = new AnnotationElement();
        annotationElement.setName(element.getName());
        annotationElement.setDataType(element.getDataType());
        annotationElement.setMin(element.getMin());
        annotationElement.setMax(element.getMax());
        annotationElement.setArray(element.isArray());
        annotationElement.setDescription(element.getDescription());
        annotationElement.setPath(element.getPath());
        annotationElement.setValueSet(element.getValueSet());
        LOG.debug("Ended: Annotation Element population");
        return annotationElement;
    }

    /**
     * Generate data type identifier for extended elements
     *
     * @param element resource element
     * @return data type identifier
     */
    private String generateExtendedElementIdentifier(Element element) {
        LOG.debug("Started: Extended Element Identifier generation");
        StringBuilder suggestedIdentifier = new StringBuilder();
        String[] tokens = element.getPath().split("\\.");
        tokens[0] = this.resourceTemplateContextInstance.getResourceName();
        for (String token : tokens) {
            suggestedIdentifier.append(CommonUtil.toCamelCase(token));
        }

        if (element.isSlice()) {
            suggestedIdentifier.append(CommonUtil.toCamelCase(element.getName()));
        } else {
            int count = 0;
            StringBuilder newIdentifier = suggestedIdentifier;
            while (this.dataTypesRegistry.contains(newIdentifier.toString())) {
                count++;
                newIdentifier = new StringBuilder(suggestedIdentifier.toString());
                newIdentifier.append(CommonUtil.toCamelCase(CommonUtil.toWords(count)));
            }
            suggestedIdentifier = newIdentifier;
        }

        this.dataTypesRegistry.add(suggestedIdentifier.toString());
        LOG.debug("Ended: Extended Element Identifier generation");
        return suggestedIdentifier.toString();
    }

    /**
     * Validates whether a resource attribute is on array of elements
     *
     * @param elementDefinition Element definition DTO for specific FHIR attribute
     * @return is an element array or not
     */
    private boolean isElementArray(ElementDefinition elementDefinition) {
        return elementDefinition.getMax().equals("*") || Integer.parseInt(elementDefinition.getMax()) > 1;
    }

    /**
     * Validates whether given string has codes
     *
     * @param string A string with/without codes delimited by pipe(|)
     * @return True or False
     */
    private boolean isCodedString(String string) {
        String[] codes = string.split(Pattern.quote("|"));
        return codes.length > 1;
    }

    public Map<String, ResourceTemplateContext> getResourceTemplateContextMap() {
        return resourceTemplateContextMap;
    }

    public Map<String, String> getResourceNameTypeMap() {
        return resourceNameTypeMap;
    }

    public Set<String> getDataTypesRegistry() {
        return dataTypesRegistry;
    }
}
