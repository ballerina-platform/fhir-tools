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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Property;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRResourceDef;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r5.model.FHIRR5ResourceDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.DataTypesRegistry;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DataTypeProfile;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ResourceDefinitionAnnotation;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ResourceTemplateContext;

import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.AbstractResourceContextGenerator;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Generator class for FHIR R5 resource related context
 */
public class R5ResourceContextGenerator extends AbstractResourceContextGenerator {
    private static final Log LOG = LogFactory.getLog(R5ResourceContextGenerator.class);

    public R5ResourceContextGenerator(BallerinaPackageGenToolConfig config, FHIRImplementationGuide ig,
                                      Map<String, DatatypeTemplateContext> datatypeTemplateContextMap) {
        super(config, ig, datatypeTemplateContextMap);
    }

    /**
     * Populate resource template contexts
     *
     * @param ig FHIR implementation guide DTO
     */
    @Override
    protected void populateResourceTemplateContexts(FHIRImplementationGuide ig) {
        LOG.debug("Started: Resource Template Context population");

        for (Map.Entry<String, FHIRResourceDef> definitionEntry : ig.getResources().entrySet()) {
            FHIRR5ResourceDef resourceDef = (FHIRR5ResourceDef) definitionEntry.getValue();
            StructureDefinition structureDefinition = (StructureDefinition) definitionEntry.getValue().getDefinition();

            if (!baseResources.contains(structureDefinition.getType())) {
                getResourceNameTypeMap().put(structureDefinition.getName(), structureDefinition.getType());

                this.resourceTemplateContextInstance = new ResourceTemplateContext();
                this.resourceTemplateContextInstance.setResourceType(structureDefinition.getType());
                this.resourceTemplateContextInstance.setResourceName(GeneratorUtils.getInstance().resolveSpecialCharacters(structureDefinition.getName()));
                this.resourceTemplateContextInstance.setProfile(resourceDef.getDefinition().getUrl());
                this.resourceTemplateContextInstance.setIgName(ig.getName());

                ResourceDefinitionAnnotation resourceDefinitionAnnotation = new ResourceDefinitionAnnotation();
                resourceDefinitionAnnotation.setName(GeneratorUtils.getInstance().resolveSpecialCharacters(structureDefinition.getName()));

                DataTypesRegistry.getInstance().addDataType(GeneratorUtils.getInstance().resolveSpecialCharacters(structureDefinition.getName()));
                resourceDefinitionAnnotation.setBaseType(CommonUtil.getSplitTokenAt(
                        structureDefinition.getBaseDefinition(),
                        ToolConstants.RESOURCE_PATH_SEPERATOR,
                        ToolConstants.TokenPosition.END)
                );

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

                    Map<String, DataTypeProfile> profiles = snapshotElement.getProfiles();
                    profiles.keySet().stream()
                            .flatMap(key -> getToolConfig().getPackageConfig().getDependentIgs().keySet().stream()
                                    .filter(key::startsWith)
                                    .map(profile -> getToolConfig().getPackageConfig().getDependentIgs().get(profile)))
                            .distinct()
                            .forEach(getDependentIgs()::add);
                }

                Set<String> resourceDependencies = this.resourceTemplateContextInstance.getResourceDependencies();
                resourceDependencies.addAll(getDependentIgs());
                this.resourceTemplateContextInstance.setResourceDependencies(resourceDependencies);

                for (Element resourceElement : this.resourceTemplateContextInstance.getResourceElements().values()) {
                    populateResourceExtendedElementsMap(resourceElement);
                    populateResourceElementAnnotationsMap(resourceElement);
                }

                for (List<Element> slices : this.resourceTemplateContextInstance.getSliceElements().values()) {
                    for (Element slice : slices) {
                        populateResourceExtendedElementsMap(slice);
                    }
                }

                getResourceTemplateContextMap().put(structureDefinition.getName(), this.resourceTemplateContextInstance);
            }
        }
        LOG.debug("Ended: Resource Template Context population");
    }

    private void populateElementDefinitionMap(List<ElementDefinition> elementDefinitions) {
        for (ElementDefinition elementDefinition : elementDefinitions) {
            String id = elementDefinition.getId();
            int colonCount = StringUtils.countMatches(id, ":");
            if (colonCount > 1) {
                // nested slice; ignore processing
                continue;
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
        boolean isReferredElement;
        String sliceNamePattern;

        for (ElementDefinition elementDefinition : elementDefinitions) {
            isSlice = false;
            isReferredElement = false;
            elementPath = elementDefinition.getPath();

            // Adding logic to handle multi datatype element definitions in the
            // format of <RESOURCE>.<field>[x]:<field><Datatype>.<childPath>
            // i.e : MedicationRequest.medication[x]:medicationCodeableConcept.coding
            String id = elementDefinition.getId();
            if (id.contains(":")) {
                if (id.substring(id.indexOf("."), id.lastIndexOf(":")).contains(":")) {
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

            if (elementDefinition.hasContentReference()) {
                isReferredElement = true;
                String contentReference = elementDefinition.getContentReference();

                String referringElementId = contentReference.split("#")[1];
                List<ElementDefinition.TypeRefComponent> referringElementTypes = elementDefinitions.stream()
                        .filter(elemDef -> elemDef.getId().equals(referringElementId))
                        .findFirst()
                        .map(ElementDefinition::getType)
                        .orElse(new ArrayList<>());

                ///  Sets the datatype to be that of the referred element temporarily
                ///  as without the ElementDefinition.TypeRefComponent, the element path
                ///  parsing is ignored.
                elementDefinition.setType(referringElementTypes);
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

                                    Element childElement = populateElement(rootElementName, elementName, type, isSlice, isReferredElement, elementDefinition);
                                    if (ToolConstants.DATA_TYPE_EXTENSION.equals(childElement.getDataType()) && !elementName.equals("extension")
                                            && !elementName.equals("modifierExtension")) {
                                        continue;
                                    }
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

                        if (elementPath.contains("[x]")) {
                            elementPath = elementPath.replace("[x]", "");
                        } else {
                            elementPath = elementPath.substring(rootElementName.length() + 1);
                        }

                        if (elementMap.containsKey(rootElementName) && elementMap.get(rootElementName).hasChildElements())
                            elementMap = elementMap.get(rootElementName).getChildElements();
                    }
                } else {
                    List<ElementDefinition.TypeRefComponent> types = elementDefinition.getType();
                    String tempElement = elementPath.split(Pattern.quote("[x]"))[0];
                    for (ElementDefinition.TypeRefComponent type : elementDefinition.getType()) {
                        if (types.size() > 1 || elementPath.contains("[x]"))
                            elementPath = tempElement + CommonUtil.toCamelCase(type.getCode());
                        Element element = populateElement(resourceName, elementPath, type, isSlice, isReferredElement, elementDefinition);
                        snapshotElementMap.put(elementPath, element);
                    }
                }
            }
        }
        this.resourceTemplateContextInstance.setSnapshotElements(snapshotElementMap);
        LOG.debug("Ended: Snapshot Element Map population");
    }

    private void populateResourceSliceElementsMap(Element element) {
        LOG.debug("Started: Resource Slice Element Map population");
        if (ToolConstants.DATA_TYPE_EXTENSION.equals(element.getDataType()) && element.isSlice()) {
            return;
        }
        if (element.hasChildElements()) {
            for (Map.Entry<String, Element> childEntry : element.getChildElements().entrySet()) {
                populateResourceSliceElementsMap(childEntry.getValue());
                if (element.isSlice()) {
                    ElementDefinition elementDefinition = (ElementDefinition) this.resourceTemplateContextInstance.getSnapshotElementDefinitions().get(childEntry.getValue().getPath());
                    if (elementDefinition != null && isElementArray(elementDefinition)) {
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

    /**
     * Create resource element
     *
     * @param rootName          element root
     * @param name              element name
     * @param type              element data type
     * @param elementDefinition element definition DTO
     * @return created element object
     */
    private Element populateElement(String rootName, String name, ElementDefinition.TypeRefComponent type, boolean isSlice, boolean isReferredElement, ElementDefinition elementDefinition) {
        LOG.debug("Started: Resource Element population");

        Element element = new Element();
        element.setName(GeneratorUtils.getInstance().resolveSpecialCharacters(name));
        element.setRootElementName(rootName);

        // Check if the element is a referred element.
        // Has to type specified but a contentReference field.
        if (isReferredElement) {
            element.setContentReference(elementDefinition.getContentReference());
        }

        if (ToolConstants.ELEMENT.equals(type.getCode())) {
            element.setDataType(ToolConstants.ELEMENT + CommonUtil.toCamelCase(name));
        } else if (isReferredElement && GeneratorUtils.isReferredFromInternational(element.getContentReference())) {
            // Referred an element from international500

            String dataType = GeneratorUtils.getReferringElementName(element.getContentReference(), true, "");
            element.setDataType(GeneratorUtils.getInstance().resolveSpecialCharacters(dataType));
        } else {
            element.setDataType(GeneratorUtils.getInstance().resolveDataType(getToolConfig(), type.getCode()));
        }
        //Adding profiles of the resource element type
        List<CanonicalType> profiles = type.getProfile();
        if (!profiles.isEmpty()) {
            for (CanonicalType profile : profiles) {
                String profileType = CommonUtil.getSplitTokenAt(profile.getValue(), "/", ToolConstants.TokenPosition.END);
                profileType = GeneratorUtils.getInstance().getUniqueIdentifierFromId(profileType);

                if (getDatatypeTemplateContextMap().containsKey(profile.getValue())) {
                    element.addProfile(profile.getValue(), profileType);
                    DataTypesRegistry.getInstance().addDataType(profileType);
                } else {
                    element.addProfile(profile.getValue(), profileType);
                }
                //check for prefix when non R5 profiles are available
                for (String dependentIgUrl : getToolConfig().getPackageConfig().getDependentIgs().keySet()) {
                    if (profile.getValue().startsWith(dependentIgUrl)) {
                        String dependentIgPackageName = getToolConfig().getPackageConfig().getDependentIgs().get(dependentIgUrl);
                        String dependentIgPackagePrefix = CommonUtil.getSplitTokenAt(dependentIgPackageName, "\\.", ToolConstants.TokenPosition.END);
                        element.getProfiles().get(profile.getValue()).setPrefix(dependentIgPackagePrefix);
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

        // This filter makes sure that elements with multiple possible datatypes are not marked as
        // required. Since we can't specify "either-or" for datatypes, all such elements are
        // treated as optional.
        if (elementDefinition.getPath().endsWith("[x]")) {
            element.setMin(0);
        } else {
            element.setMin(elementDefinition.getMin());
        }

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
            GeneratorUtils.populateCodeValuesForCodeElements(elementDefinition.getShort(), element);
        }
//        markConstrainedElements(element);
        LOG.debug("Ended: Resource Element population");
        return element;
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

    private Element populateChildElementProperties(Property childProperty, String elementPath) {
        Element childElement = new Element();
        childElement.setName(childProperty.getName());
        childElement.setDataType(childProperty.getTypeCode());
        childElement.setArray(childProperty.isList());
        childElement.setMin(1);
        childElement.setMax(childProperty.getMaxCardinality());
        childElement.setDescription(CommonUtil.parseMultilineString(childProperty.getDefinition()));
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

    /**
     * Validates whether a resource attribute is on array of elements
     *
     * @param elementDefinition Element definition DTO for specific FHIR attribute
     * @return is an element array or not
     */
    private boolean isElementArray(ElementDefinition elementDefinition) {
        return "*".equals(elementDefinition.getBase().getMax()) || Integer.parseInt(elementDefinition.getBase().getMax()) > 1;
    }
}
