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
import org.wso2.healthcare.fhir.codegen.tool.lib.model.FHIRImplementationGuide;
import org.wso2.healthcare.fhir.codegen.tool.lib.model.FHIRResourceDef;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants.CONSTRAINTS_LIB_IMPORT;

/**
 * Generator class for resource related context
 */
public class ResourceContextGenerator {
    private static final Log LOG = LogFactory.getLog(ResourceContextGenerator.class);
    private final BallerinaPackageGenToolConfig toolConfig;
    private final Map<String, ResourceTemplateContext> resourceTemplateContextMap;
    private final Map<String, String> resourceNameTypeMap;
    private final Set<String> dataTypesRegistry;
    private HashMap<String, ExtendedElement> resourceExtendedElementMap;

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
            if (definitionEntry.getValue().getDefinition().getKind().name().equalsIgnoreCase("RESOURCE") &&
                    checkEnabledFHIRResources(structureDefinition, ig.getName())) {

                this.resourceExtendedElementMap = new HashMap<>();

                ResourceTemplateContext resourceTemplateContext = new ResourceTemplateContext();
                resourceTemplateContext.setResourceType(structureDefinition.getType());
                resourceTemplateContext.setResourceName(structureDefinition.getName());
                resourceTemplateContext.setProfile(definitionEntry.getValue().getDefinition().getUrl());
                resourceTemplateContext.setIgName(ig.getName());
                resourceTemplateContext.setBaseIgName(toolConfig.getIncludedIGConfigs().get(ig.getName()).getBaseIGPackage());

                ResourceDefinitionAnnotation resourceDefinitionAnnotation = new ResourceDefinitionAnnotation();
                resourceDefinitionAnnotation.setName(structureDefinition.getName());
                resourceDefinitionAnnotation.setBaseType(CommonUtil.getSplitTokenAt(structureDefinition
                        .getBaseDefinition(), File.separator, ToolConstants.TokenPosition.END));
                resourceDefinitionAnnotation.setProfile(resourceTemplateContext.getProfile());

                populateResourceElementMap(structureDefinition.getSnapshot().getElement(), resourceTemplateContext);

                HashMap<String, AnnotationElement> annotationElements = new HashMap<>();
                for (Element resourceElement : resourceTemplateContext.getElements().values()) {
                    populateExtendedElementsMap(resourceElement);
                    AnnotationElement annotationElement = populateAnnotationElement(resourceElement);
                    annotationElements.put(resourceElement.getName(), annotationElement);
                    annotationElements.put(annotationElement.getName(), annotationElement);
                }
                resourceTemplateContext.setExtendedElements(this.resourceExtendedElementMap);
                resourceDefinitionAnnotation.setElements(annotationElements);
                resourceTemplateContext.setResourceDefinitionAnnotation(resourceDefinitionAnnotation);

                this.resourceNameTypeMap.put(structureDefinition.getName(), structureDefinition.getType());
                this.resourceTemplateContextMap.put(structureDefinition.getName(), resourceTemplateContext);
            }
        }
        LOG.debug("Ended: Resource Template Context population");
    }

    /**
     * Populate resource elements map in a hierarchical way
     *
     * @param elementDefinitions      FHIR element definition DTO
     * @param resourceTemplateContext resource template context
     */
    private void populateResourceElementMap(List<ElementDefinition> elementDefinitions,
                                            ResourceTemplateContext resourceTemplateContext) {
        LOG.debug("Started: Resource Element Map population");
        elementDefinitions.sort(new Comparator<ElementDefinition>() {
            @Override
            public int compare(ElementDefinition e1, ElementDefinition e2) {
                return e1.getPath().compareToIgnoreCase(e2.getPath());
            }
        });

        String elementPath;
        String[] elementPathTokens;
        HashMap<String, Element> resourceElementMap = new HashMap<>();

        boolean isConstraintsImportExists = false;
        for (ElementDefinition elementDefinition : elementDefinitions) {

            isConstraintsImportExists = isConstraintsImportExists || resourceTemplateContext.getResourceDependencies()
                    .stream()
                    .filter(d -> d.equals(CONSTRAINTS_LIB_IMPORT))
                    .findAny().isPresent();

            if (!isConstraintsImportExists && elementDefinition.getMin() == 1 && elementDefinition.getMax().equals("*")) {
                Set<String> resourceDependencies = resourceTemplateContext.getResourceDependencies();
                resourceDependencies.add(CONSTRAINTS_LIB_IMPORT);
            }
            elementPath = elementDefinition.getPath();
            // Adding logic to handle multi datatype element definitions in the
            // format of <RESOURCE>.<field>[x]:<field><Datatype>.<childPath>
            // i.e : MedicationRequest.medication[x]:medicationCodeableConcept.coding
            String id = elementDefinition.getId();
            if (id.contains("[x]:")) {
                String multiTypeFieldFHIRPath = id.substring(id.lastIndexOf("[x]:") + 4);
                String multiTypeFieldResourcePart = id.substring(0, id.lastIndexOf("[x]:"));
                if (multiTypeFieldFHIRPath.contains(".")) {
                    String multiDataTypeParentFHIRPath = multiTypeFieldResourcePart.substring(0,
                            multiTypeFieldResourcePart.lastIndexOf(".") + 1);
                    elementPath = multiDataTypeParentFHIRPath.concat(multiTypeFieldFHIRPath);
                }
            }
            elementPathTokens = elementPath.split("\\.");

            if (elementPathTokens.length > 1) {
                String rootElementName;
                String elementName;
                String resourceName = elementPathTokens[0];
                elementPath = elementPath.substring(resourceName.length() + 1);

                if (elementPath.split("\\.").length > 1) {
                    Map<String, Element> elementMap = resourceElementMap;
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
                                    if (types.size() > 1)
                                        elementName = tempElement + CommonUtil.toCamelCase(type.getCode());
                                    Element childElement = populateElement(rootElementName, elementName, type.getCode(), elementDefinition);
                                    if (rootElement.getChildElements() != null) {
                                        if (!rootElement.getChildElements().containsKey(elementName)) {
                                            rootElement.getChildElements().put(elementName, childElement);
                                        } else {
                                            HashMap<String, Element> newChildElements = new HashMap<>();
                                            newChildElements.put(elementName, childElement);
                                            rootElement.setChildElements(newChildElements);
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
                        if (elementMap.get(rootElementName).isHasChildElements())
                            elementMap = elementMap.get(rootElementName).getChildElements();
                    }
                } else {
                    List<ElementDefinition.TypeRefComponent> types = elementDefinition.getType();
                    String tempElement = elementPath.split(Pattern.quote("[x]"))[0];
                    for (ElementDefinition.TypeRefComponent type : elementDefinition.getType()) {
                        if (types.size() > 1)
                            elementPath = tempElement + CommonUtil.toCamelCase(type.getCode());
                        if (!elementDefinition.getId().equals(elementDefinition.getPath())) {
                            if(elementDefinition.getSliceName() != null)
                                elementPath = elementDefinition.getSliceName();
                        }
                        Element element = populateElement(resourceName, elementPath, type.getCode(), elementDefinition);
                        resourceElementMap.put(elementPath, element);
                    }
                }
            }
        }
        resourceTemplateContext.setElements(resourceElementMap);
        LOG.debug("Ended: Resource Element Map population");
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
    private Element populateElement(String rootName, String name, String type, ElementDefinition elementDefinition) {
        LOG.debug("Started: Resource Element population");
        Element element = new Element();
        element.setName(name);
        element.setRootElementName(rootName);
        element.setDataType(GeneratorUtils.resolveDataType(toolConfig, type));
        element.setMin(String.valueOf(elementDefinition.getMin()));
        element.setMax(elementDefinition.getMax());
        element.setArray(isElementArray(elementDefinition));
        element.setPath(elementDefinition.getPath());
        element.setRequired(elementDefinition.getMin() > 0);
        element.setValueSet(elementDefinition.getBinding().getValueSet());
        element.setDescription(CommonUtil.parseMultilineString(elementDefinition.getDefinition()));

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
        LOG.debug("Ended: Resource Element population");
        return element;
    }

    /**
     * Populate extended elements map
     *
     * @param element resource element
     */
    private void populateExtendedElementsMap(Element element) {
        LOG.debug("Started: Resource Extended Element Map population");
        if (element.isHasChildElements()) {
            for (Map.Entry<String, Element> childEntry : element.getChildElements().entrySet()) {
                populateExtendedElementsMap(childEntry.getValue());
            }
            validateAndPopulateExtendedElement(this.resourceExtendedElementMap, element);
        }
        LOG.debug("Ended: Resource Extended Element Map population");
    }

    /**
     * Validate and create extended elements from resource elements
     *
     * @param extendedElementMap extended elements map
     * @param element            resource element to be validated
     */
    private void validateAndPopulateExtendedElement(HashMap<String, ExtendedElement> extendedElementMap,
                                                    Element element) {
        LOG.debug("Started: Resource Extended Element validation");
        ExtendedElement extendedElement = null;
        String elementDataType = element.getDataType();
        if (elementDataType.equals("code") && element.isHasChildElements()) {
            extendedElement = populateExtendedElement(element, BallerinaDataType.Enum);
        } else if (elementDataType.equals("BackboneElement")) {
            extendedElement = populateExtendedElement(element, BallerinaDataType.Record);
            extendedElement.setElements(element.getChildElements());

            DataTypeDefinitionAnnotation annotation = new DataTypeDefinitionAnnotation();
            annotation.setName(extendedElement.getTypeName());

            HashMap<String, AnnotationElement> annotationElementMap = new HashMap<>();
            for (Element subElement : element.getChildElements().values()) {
                AnnotationElement annotationElement = populateAnnotationElement(subElement);
                annotationElementMap.put(annotationElement.getName(), annotationElement);
            }
            annotation.setElements(annotationElementMap);
            extendedElement.setAnnotation(annotation);
        }
        if (extendedElement != null) {
            boolean isAlreadyExists = false;
            for (Map.Entry<String, ExtendedElement> elementEntry : extendedElementMap.entrySet()) {
                isAlreadyExists = elementEntry.getValue().equals(extendedElement);
                if (isAlreadyExists) {
                    element.setDataType(elementEntry.getValue().getTypeName());
                    break;
                }
            }
            if (!isAlreadyExists)
                extendedElementMap.put(extendedElement.getTypeName(), extendedElement);
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
    private ExtendedElement populateExtendedElement(Element element, BallerinaDataType balType) {
        LOG.debug("Started: Resource Extended Element population");
        ExtendedElement extendedElement = new ExtendedElement();
        String extendedElementTypeName = generateExtendedElementIdentifier(element);
        extendedElement.setTypeName(extendedElementTypeName);
        element.setDataType(extendedElementTypeName);
        extendedElement.setBalDataType(balType);
        if (element.getChildElements() != null)
            extendedElement.setElements(element.getChildElements());

        LOG.debug("Ended: Resource Extended Element population");
        return extendedElement;
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
        for (String token : tokens) {
            suggestedIdentifier.append(CommonUtil.toCamelCase(token));
        }

        int count = 0;
        StringBuilder newIdentifier = suggestedIdentifier;
        while (this.dataTypesRegistry.contains(newIdentifier.toString())) {
            count++;
            newIdentifier = new StringBuilder(suggestedIdentifier.toString());
            newIdentifier.append(CommonUtil.toCamelCase(CommonUtil.toWords(count)));
        }
        suggestedIdentifier = newIdentifier;

        this.dataTypesRegistry.add(suggestedIdentifier.toString());
        LOG.debug("Ended: Extended Element Identifier generation");
        return suggestedIdentifier.toString();
    }

    /**
     * Check the config and filter profiles before generating
     *
     * @param structureDefinition FHIR structure definition
     * @param igName              IG name
     */
    public boolean checkEnabledFHIRResources(StructureDefinition structureDefinition, String igName) {
        String url = structureDefinition.getUrl();

        if (this.toolConfig.getIncludedIGConfigs().get(igName).getIncludedProfiles().isEmpty()) {
            return !this.toolConfig.getIncludedIGConfigs().get(igName).getExcludedProfiles().contains(url);
        } else {
            return this.toolConfig.getIncludedIGConfigs().get(igName).getIncludedProfiles().contains(url);
        }
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
