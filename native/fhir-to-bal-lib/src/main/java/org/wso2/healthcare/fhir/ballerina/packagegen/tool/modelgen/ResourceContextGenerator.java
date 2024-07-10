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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Base;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRResourceDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.DataTypesRegistry;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.AnnotationElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.BallerinaDataType;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DataTypeDefinitionAnnotation;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
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
    public final Set<String> baseResources = new HashSet<>(Arrays.asList("Bundle", "OperationOutcome", "CodeSystem",
            "ValueSet", "DomainResource", "Resource"));
    private final BallerinaPackageGenToolConfig toolConfig;
    private ResourceTemplateContext resourceTemplateContextInstance;
    private final Map<String, ResourceTemplateContext> resourceTemplateContextMap;
    private final Map<String, String> resourceNameTypeMap;

    private final Map<String, DatatypeTemplateContext> datatypeTemplateContextMap;

    public ResourceContextGenerator(BallerinaPackageGenToolConfig config, FHIRImplementationGuide ig,
                                    Map<String, DatatypeTemplateContext> datatypeTemplateContextMap) {
        LOG.debug("Resource Context Generator Initiated");
        this.toolConfig = config;
        this.resourceTemplateContextMap = new HashMap<>();
        this.resourceNameTypeMap = new HashMap<>();
        this.datatypeTemplateContextMap = datatypeTemplateContextMap;
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
                this.resourceTemplateContextInstance.setResourceName(GeneratorUtils.getInstance().resolveSpecialCharacters(structureDefinition.getName()));
                this.resourceTemplateContextInstance.setProfile(definitionEntry.getValue().getDefinition().getUrl());
                this.resourceTemplateContextInstance.setIgName(ig.getName());

                ResourceDefinitionAnnotation resourceDefinitionAnnotation = new ResourceDefinitionAnnotation();
                resourceDefinitionAnnotation.setName(GeneratorUtils.getInstance().resolveSpecialCharacters(structureDefinition.getName()));
                DataTypesRegistry.getInstance().addDataType(GeneratorUtils.getInstance().resolveSpecialCharacters(structureDefinition.getName()));
                resourceDefinitionAnnotation.setBaseType(CommonUtil.getSplitTokenAt(structureDefinition
                        .getBaseDefinition(), ToolConstants.RESOURCE_PATH_SEPERATOR, ToolConstants.TokenPosition.END));
                resourceDefinitionAnnotation.setProfile(this.resourceTemplateContextInstance.getProfile());
                resourceDefinitionAnnotation.setElements(new HashMap<>());
                this.resourceTemplateContextInstance.setResourceDefinitionAnnotation(resourceDefinitionAnnotation);

                populateElementDefinitionMap(structureDefinition.getSnapshot().getElement());
                populateSnapshotElementMap(structureDefinition.getSnapshot().getElement());
                populateDifferentialElementIdsList(structureDefinition.getDifferential().getElement());

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

    private void populateElementDefinitionMap(List<ElementDefinition> elementDefinitions) {
        for (ElementDefinition elementDefinition : elementDefinitions) {
            String id = elementDefinition.getId();
            if (id.contains(":")) {
                if (id.substring(id.indexOf("."), id.lastIndexOf(":")).contains(":")) {
                    //todo: rewrite the logic using regex

                    // nested slice; ignore processing
                    continue;
                }
            }
            this.resourceTemplateContextInstance.getSnapshotElementDefinitions().put(id, elementDefinition);
        }
    }

    /**
     * Populate resource elements map in a hierarchical way
     *
     * @param elementDefinitions FHIR element definition DTO
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
            if (id.contains(":")) {
                if (id.substring(id.indexOf("."), id.lastIndexOf(":")).contains(":")) {
                    //todo: rewrite the logic using regex

                    // nested slice; ignore processing
                    continue;
                }
            }
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
                if (id.endsWith(sliceNamePattern)) {
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
                        if (rootElementName.contains("[x]"))
                            rootElementName = rootElementName.replace("[x]", elementDefinition.getBase().getPath().split("\\.")[0]);

                        if (elementPathTokens.length == 2) {
                            elementName = elementPathTokens[1];
                            if (elementMap.containsKey(rootElementName)) {
                                Element rootElement = elementMap.get(rootElementName);
                                List<ElementDefinition.TypeRefComponent> types = elementDefinition.getType();
                                String tempElement = elementName.split(Pattern.quote("[x]"))[0];
                                for (ElementDefinition.TypeRefComponent type : elementDefinition.getType()) {
                                    if (types.size() > 1 || elementName.contains("[x]"))
                                        elementName = tempElement + CommonUtil.toCamelCase(type.getCode());

                                    Element childElement = populateElement(rootElementName, elementName, type, isSlice, elementDefinition);
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
                        if (elementPath.contains("[x]"))
                            elementPath = elementPath.replace("[x]", elementDefinition.getBase().getPath().split("\\.")[0]);
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
                        Element element = populateElement(resourceName, elementPath, type, isSlice, elementDefinition);
                        snapshotElementMap.put(elementPath, element);
                    }
                }
            }
        }
        this.resourceTemplateContextInstance.setSnapshotElements(snapshotElementMap);
        LOG.debug("Ended: Snapshot Element Map population");
    }

    private void populateDifferentialElementIdsList(List<ElementDefinition> elementDefinitions) {
        String elementPath;
        for (ElementDefinition elementDefinition : elementDefinitions) {
            elementPath = elementDefinition.getPath();
            String relativePath = elementPath.replace(this.resourceTemplateContextInstance.getResourceType() + ".", "");
            String[] pathTokens = relativePath.split("\\.");
            if (pathTokens.length > 1) {
                this.resourceTemplateContextInstance.getDifferentialElementIds().add(pathTokens[0]);
            }
        }
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
    private Element populateElement(String rootName, String name, ElementDefinition.TypeRefComponent type, boolean isSlice, ElementDefinition elementDefinition) {
        LOG.debug("Started: Resource Element population");
        Element element = new Element();
        element.setName(GeneratorUtils.getInstance().resolveSpecialCharacters(name));
        element.setRootElementName(rootName);
        if (ToolConstants.ELEMENT.equals(type.getCode())) {
            element.setDataType(ToolConstants.ELEMENT + CommonUtil.toCamelCase(name));
        } else {
            element.setDataType(GeneratorUtils.getInstance().resolveDataType(toolConfig, type.getCode()));
        }
        //Adding profiles of the resource element type
        List<CanonicalType> profiles = type.getProfile();
        if (!profiles.isEmpty()) {
            for (CanonicalType profile : profiles) {
                String profileType = CommonUtil.getSplitTokenAt(profile.getValue(), "/", ToolConstants.TokenPosition.END);
                profileType = GeneratorUtils.getInstance().getUniqueIdentifierFromId(profileType);
                if (datatypeTemplateContextMap.containsKey(profile.getValue())) {
                    element.addProfile(profile.getValue(), profileType);
                    DataTypesRegistry.getInstance().addDataType(profileType);
                } else {
                    element.addProfile(profile.getValue(), profileType);
                }
                //check for prefix when non R4 profiles are available
                if (!profile.getValue().startsWith(ToolConstants.FHIR_R4_DEFINITION_URL)) {
                    if (!StringUtils.isEmpty(toolConfig.getPackageConfig().getParentPackage())) {
                        String prefix = CommonUtil.getSplitTokenAt(toolConfig.getPackageConfig().getParentPackage(), "\\.", ToolConstants.TokenPosition.END);
                        element.getProfiles().get(profile.getValue()).setPrefix(prefix);
                    }
                }
            }
        } else {
            element.addProfile(element.getDataType(), element.getDataType());
        }
        if (elementDefinition.hasFixed()) {
            ArrayList<String> values = new ArrayList<>();
            values.add(elementDefinition.getFixed().primitiveValue());
            element.setFixedValue(values);
        } else if (elementDefinition.hasPattern() && !elementDefinition.getPattern().children().isEmpty()) {
            HashMap<String, Element> childElements = new HashMap<>();
            for (Property childProperty : elementDefinition.getPattern().children()) {
                if (childProperty.hasValues()) {
                    Element childElement = populateChildElementProperties(childProperty, elementDefinition.getPath());
                    childElements.put(childProperty.getName(), childElement);
                }
            }
            element.setChildElements(childElements);
        }

        element.setMin(elementDefinition.getMin());
        element.setMax(GeneratorUtils.getMaxCardinality(elementDefinition.getMax()));
        element.setArray(isElementArray(elementDefinition));
        element.setIsSlice(isSlice);
        element.setPath(elementDefinition.getPath());
        element.setValueSet(elementDefinition.getBinding().getValueSet());
        element.setDescription(CommonUtil.parseMultilineString(elementDefinition.getDefinition()));
        element.setSummary(CommonUtil.parseMultilineString(elementDefinition.getShort()));
        element.setRequirement(CommonUtil.parseMultilineString(elementDefinition.getRequirements()));

        /*
         Todo: Fix resolving of Codes from implementation Guide
         Refer Issue: https://github.com/wso2-enterprise/open-healthcare/issues/928
         */
        if (element.getDataType().equals("code")) {
            GeneratorUtils.populateCodeValuesForCodeElements(elementDefinition, element);
        }
//        markConstrainedElements(element);
        LOG.debug("Ended: Resource Element population");
        return element;
    }

    private Element populateChildElementProperties(Property childProperty, String elementPath) {
        Element childElement = new Element();
        childElement.setName(childProperty.getName());
        childElement.setDataType(childProperty.getTypeCode());
        childElement.setArray(childProperty.isList());
        childElement.setMin(1);
        childElement.setMax(childProperty.getMaxCardinality());
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
//        markConstrainedElements(childElement);
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
            markConstrainedElements(element);
            this.resourceTemplateContextInstance.getResourceElements().put(element.getName(), element);
        }
    }

    private void markConstrainedElements(Element element) {
        boolean isCardinalityConstrained = (element.getMin() >= 1) && (element.getMax() > 1);
        boolean isConstraintsImportExists = this.resourceTemplateContextInstance.getResourceDependencies()
                .stream()
                .anyMatch(d -> d.equals(CONSTRAINTS_LIB_IMPORT));

        if (!isConstraintsImportExists && isCardinalityConstrained) {
            this.resourceTemplateContextInstance.getResourceDependencies().add(CONSTRAINTS_LIB_IMPORT);
        }
    }

    private void markExtendedElements(Element element) {
        if (!"Extension".equals(element.getDataType())) {
            if (this.resourceTemplateContextInstance.getDifferentialElementIds().contains(element.getName()) || "Code".equals(element.getDataType()) || "BackboneElement".equals(element.getDataType()) || element.hasFixedValue()) {
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
     * @param element resource element to be validated
     */
    private void validateAndPopulateExtendedElement(Element element) {
        LOG.debug("Started: Resource Extended Element validation");
        ExtendedElement extendedElement;
        String elementDataType = element.getDataType();
        if (elementDataType.equals("code") && element.hasChildElements()) {
            extendedElement = GeneratorUtils.getInstance().populateExtendedElement(element, BallerinaDataType.Enum, elementDataType,
                    this.resourceTemplateContextInstance.getResourceName());
            putExtendedElementIfAbsent(element, extendedElement);
        } else if (element.isSlice() || elementDataType.equals("BackboneElement") || (element.isExtended() && element.hasChildElements())) {
            markConstrainedElements(element);
            extendedElement = GeneratorUtils.getInstance().populateExtendedElement(element, BallerinaDataType.Record, elementDataType,
                    this.resourceTemplateContextInstance.getResourceName());
            extendedElement.setElements(element.getChildElements());

            DataTypeDefinitionAnnotation annotation = new DataTypeDefinitionAnnotation();
            annotation.setName(extendedElement.getTypeName());

            if (element.hasChildElements()) {
                HashMap<String, AnnotationElement> childElementAnnotations = new HashMap<>();
                for (Element subElement : element.getChildElements().values()) {
                    markConstrainedElements(subElement);
                    AnnotationElement annotationElement = GeneratorUtils.getInstance().populateAnnotationElement(subElement);
                    childElementAnnotations.put(annotationElement.getName(), annotationElement);
                }
                annotation.setElements(childElementAnnotations);
            }
            extendedElement.setAnnotation(annotation);
            if (!element.isSlice() && this.resourceTemplateContextInstance.getSliceElements().containsKey(element.getPath())) {
                for (Element slice : this.resourceTemplateContextInstance.getSliceElements().get(element.getPath())) {
                    slice.setDataType(extendedElement.getTypeName());
                }
            }
            putExtendedElementIfAbsent(element, extendedElement);
        }
        LOG.debug("Ended: Resource Extended Element validation");
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
                if (element.isSlice()) {
                    ElementDefinition elementDefinition = this.resourceTemplateContextInstance.getSnapshotElementDefinitions().get(childEntry.getValue().getPath());
                    if (elementDefinition != null && "*".equals(elementDefinition.getMax()) && childEntry.getValue().getMax() != Integer.MAX_VALUE) {
                        childEntry.getValue().setArray(true);
                    }
                }
            }
        }

        if (element.isSlice()) {
            if (this.resourceTemplateContextInstance.getSliceElements().get(element.getPath()) != null) {
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
        AnnotationElement annotationElement = GeneratorUtils.getInstance().populateAnnotationElement(element);
        this.resourceTemplateContextInstance.getResourceDefinitionAnnotation().getElements().put(element.getName(), annotationElement);
        this.resourceTemplateContextInstance.getResourceDefinitionAnnotation().getElements().put(annotationElement.getName(), annotationElement);
        LOG.debug("Ended: Resource Element Annotation Map population");
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
}
