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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.DataTypesRegistry;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.AnnotationElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.BallerinaDataType;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ExtendedElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.SearchParameter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GeneratorUtils {
    private static final Log LOG = LogFactory.getLog(GeneratorUtils.class);

    private BallerinaPackageGenToolConfig toolConfig;

    private final ArrayList<String> NON_TYPE_INCLUSION_DATA_TYPES = new ArrayList<>(List.of("Extension"));

    private final HashMap<String, String> DATA_TYPE_MAP = new HashMap<>() {{
        put("http://hl7.org/fhirpath/System.String", "string");
        put("http://hl7.org/fhirpath/System.Date", "date");
        put("url", "urlType");
    }};

    private final HashMap<String, String> SPECIAL_CHARACTERS_MAP = new HashMap<>() {{
        put("=", "equal");
        put("!=", "not_equal");
        put(">", "greater_than");
        put(">=", "greater_than_or_equal");
        put("<", "less_than");
        put("<=", "less_than_or_equal");
    }};

    // Ballerina Keywords
    // https://github.com/ballerina-platform/ballerina-lang/blob/v2201.7.0/compiler/ballerina-parser/src/main/java/io/ballerina/compiler/internal/parser/LexerTerminals.java
    private final HashMap<String, String> KEYWORD_CONFLICTS_MAP = new HashMap<>() {{
        put("type", "'type");
        put("source", "'source");
        put("client", "'client");
        put("resource", "'resource");
        put("order", "'order");
        put("class", "'class");
        put("version", "'version");
        put("final", "'final");
        put("error", "'error");
        put("parameter", "'parameter");
        put("start", "'start");
        put("transaction", "'transaction");
        put("json", "_json");
        put("service", "'service");
        put("function", "'function");
        put("fail", "'fail");
        put("in", "'in");
        put("abstract", "'abstract");
        put("import", "'import");
        put("string", "_string");
        put("from", "'from");
        put("boolean", "'boolean");
        put("outer", "'outer");
        put("never", "'never");
        put("on", "'on");
        put("decimal", "'decimal");
        put("limit", "'limit");
        put("check", "'check");
        put("field", "'field");
        put("map", "'map");
    }};

    private final HashMap<String, HashMap<String, String>> VALUESET_DATA_TYPES = new HashMap<>() {{
        put("ElementDefinition", new HashMap<>() {{
            put("'type", "ElementType");
            put("mapping", "ElementMapping");
            put("binding", "ElementBinding");
            put("example", "ElementExample");
            put("slicing", "ElementSlicing");
            put("constraint", "ElementConstraint");
            put("base", "ElementBase");
        }});
        put("Address", new HashMap<>() {{
            put("'type", "AddressType");
            put("use", "AddressUse");
        }});
    }};

    private static final GeneratorUtils instance = new GeneratorUtils();

    public static GeneratorUtils getInstance() {
        return instance;
    }

    public void setToolConfig(BallerinaPackageGenToolConfig toolConfig) {
        instance.toolConfig = toolConfig;
    }

    /**
     * Check whether the data type should be replaced by a ballerina type.
     *
     * @param dataType data type identifier from FHIR structure definition
     * @return true if the data type should be replaced by a ballerina type
     */
    public boolean shouldReplacedByBalType(String dataType) {
        return DATA_TYPE_MAP.containsKey(dataType);
    }

    public String getTypeWithImport(String dataType) {
        return DataTypesRegistry.getInstance().containsDataType(dataType) ? dataType : getBasePackageIdentifier() + dataType;
    }

    public String getBasePackageIdentifier() {
        String basePackageIdentifier = "";
        if (toolConfig.getPackageConfig().getBasePackage() != null) {
            String basePackage = toolConfig.getPackageConfig().getBasePackage();
            basePackageIdentifier = basePackage.substring(basePackage.lastIndexOf(".") + 1) + ":";
        }
        return basePackageIdentifier;
    }

    /**
     * Resolve for specific data types with preferred type from config.
     *
     * @param fhirDataType data type identifier from FHIR structure definition
     * @return preferred replacement from config
     */
    public String resolveDataType(BallerinaPackageGenToolConfig toolConfig, String fhirDataType) {
        if (DATA_TYPE_MAP.containsKey(fhirDataType)) {
            return DATA_TYPE_MAP.get(fhirDataType);
        } else if (toolConfig != null && toolConfig.getDataTypeMappingConfigs().containsKey(fhirDataType))
            return toolConfig.getDataTypeMappingConfigs().get(fhirDataType).getBallerinaType();
        else
            return fhirDataType;
    }

    /**
     * Resolve for specific data types with preferred type from config.
     *
     * @param fhirDataType data type identifier from FHIR structure definition
     * @return ballerina type
     */
    public String resolveDataType(String fhirDataType) {
        return DATA_TYPE_MAP.getOrDefault(fhirDataType, fhirDataType);
    }

    /**
     * Filter search parameters by profiles
     *
     * @param searchParameters search parameters
     * @param profiles         profiles
     * @return filtered search parameters
     */
    public static Map<String, Map<String, SearchParameter>> filterSearchParametersByProfiles(Map<String, Map<String,
            SearchParameter>> searchParameters, List<String> profiles) {
        Map<String, Map<String, SearchParameter>> newSearchParameterMap = new HashMap<>();
        for (Map.Entry<String, Map<String, SearchParameter>> entryMap : searchParameters.entrySet()) {
            Map<String, SearchParameter> newSearchParameterTypeMap;
            for (Map.Entry<String, SearchParameter> entry : entryMap.getValue().entrySet()) {
                ArrayList<String> newBases = new ArrayList<>();
                StringBuilder newExpression = new StringBuilder();
                for (String base : entry.getValue().getBase()) {
                    if (profiles.contains(base)) {
                        newBases.add(base);
                    }

                    for (String expression : entry.getValue().getExpression().split(Pattern.quote("|"))) {
                        if (expression.contains(base)) {
                            if (!newExpression.toString().isEmpty()) {
                                newExpression.append(" | ").append(expression);
                            } else {
                                newExpression = new StringBuilder(expression);
                            }
                        }
                    }
                }

                SearchParameter searchParameter = new SearchParameter();
                if (!newBases.isEmpty()) {
                    searchParameter.setName(entry.getValue().getName());
                    searchParameter.setType(entry.getValue().getType());
                    searchParameter.setBase(newBases);
                    searchParameter.setExpression(newExpression.toString());
                    String nameTypeKey = entry.getValue().getName() + entry.getValue().getType();

                    if (newSearchParameterMap.containsKey(entry.getValue().getName())) {
                        newSearchParameterTypeMap = newSearchParameterMap.get(entry.getValue().getName());
                        newSearchParameterTypeMap.put(nameTypeKey, searchParameter);
                    } else {
                        newSearchParameterTypeMap = new HashMap<>();
                        newSearchParameterTypeMap.put(nameTypeKey, searchParameter);
                        newSearchParameterMap.put(entry.getValue().getName(), newSearchParameterTypeMap);
                    }
                }
            }
        }
        return newSearchParameterMap;
    }

    /**
     * Check whether the data type is a primitive element
     *
     * @param dataType data type
     * @return true if the data type is a primitive element
     */
    public static boolean isPrimitiveElement(String dataType) {
        return dataType.equals("string") || dataType.equals("boolean") || dataType.equals("integer") ||
                dataType.equals("decimal") || dataType.equals("uri") || dataType.equals("url") ||
                dataType.equals("canonical") || dataType.equals("base64Binary") || dataType.equals("instant") ||
                dataType.equals("date") || dataType.equals("dateTime") || dataType.equals("time") ||
                dataType.equals("code") || dataType.equals("oid") || dataType.equals("id") ||
                dataType.equals("markdown") || dataType.equals("unsignedInt") || dataType.equals("positiveInt") ||
                dataType.equals("integer64") || dataType.equals("uuid");
    }

    public static int getMaxCardinality(String maxCardinality) {
        if (maxCardinality.equals("*")) {
            return Integer.MAX_VALUE;
        } else {
            return Integer.parseInt(maxCardinality);
        }
    }

    /**
     * Create annotation element object
     *
     * @param element resource/datatype element
     * @return annotation element object
     */
    public AnnotationElement populateAnnotationElement(Element element) {
        LOG.debug("Started: Annotation Element population");
        AnnotationElement annotationElement = new AnnotationElement();
        annotationElement.setName(element.getName());
        annotationElement.setDataType(element.getDataType());
        annotationElement.setMin(String.valueOf(element.getMin()));
        annotationElement.setMax(element.getMax() == Integer.MAX_VALUE ? "*" : String.valueOf(element.getMax()));
        annotationElement.setArray(element.isArray());
        annotationElement.setDescription(element.getDescription());
        annotationElement.setPath(element.getPath());
        annotationElement.setValueSet(element.getValueSet());
        annotationElement.setExtended(element.isExtended());
        LOG.debug("Ended: Annotation Element population");
        return annotationElement;
    }

    /**
     * Populates available code values for a given code element.
     *
     * @param elementDefinition element definition from FHIR specification
     * @param element           element model for template context
     */
    public static void populateCodeValuesForCodeElements(ElementDefinition elementDefinition, Element element) {
        if (!elementDefinition.getShort().contains("|")) {
            return;
        }
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

    /**
     * Create extended element object
     *
     * @param element resource element
     * @param balType Preferred Ballerina data type for the extended element
     * @return created extended element object
     */
    public ExtendedElement populateExtendedElement(Element element, BallerinaDataType balType, String baseType,
                                                   String typeNamePrefix) {
        LOG.debug("Started: Resource Extended Element population");
        ExtendedElement extendedElement = new ExtendedElement();
        String extendedElementTypeName = generateExtendedElementIdentifier(element, typeNamePrefix);
        extendedElement.setTypeName(extendedElementTypeName);
        if (element.getProfiles() != null && element.getProfiles().containsKey(element.getDataType())) {
            element.getProfiles().get(element.getDataType()).setProfileType(extendedElement.getTypeName());
        }
        element.setDataType(extendedElement.getTypeName());
        extendedElement.setBalDataType(balType);
        extendedElement.setPath(element.getPath());
        if (element.getChildElements() != null) {
            extendedElement.setElements(element.getChildElements());
        }
        if (!GeneratorUtils.isPrimitiveElement(baseType))
            extendedElement.setBaseType(baseType);
        LOG.debug("Ended: Resource Extended Element population");
        return extendedElement;
    }

    /**
     * Generate data type identifier for extended elements
     *
     * @param element resource element
     * @param prefix  prefix for to identify resource/datatype name
     * @return data type identifier
     */
    public String generateExtendedElementIdentifier(Element element, String prefix) {
        LOG.debug("Started: Extended Element Identifier generation");
        StringBuilder suggestedIdentifier = new StringBuilder();
        String[] tokens = element.getPath().split("\\.");
        tokens[0] = prefix;
        for (String token : tokens) {
            suggestedIdentifier.append(CommonUtil.toCamelCase(token));
        }

        if (element.isSlice()) {
            suggestedIdentifier.append(CommonUtil.toCamelCase(element.getName()));
        } else {
            int count = 0;
            StringBuilder newIdentifier = suggestedIdentifier;
            while (DataTypesRegistry.getInstance().containsDataType(newIdentifier.toString())) {
                count++;
                newIdentifier = new StringBuilder(suggestedIdentifier.toString());
                newIdentifier.append(CommonUtil.toCamelCase(CommonUtil.toWords(count)));
            }
            suggestedIdentifier = newIdentifier;
        }

        String sanitizedIdentifier = resolveSpecialCharacters(suggestedIdentifier.toString());
        DataTypesRegistry.getInstance().addDataType(sanitizedIdentifier);
        LOG.debug("Ended: Extended Element Identifier generation");
        return sanitizedIdentifier;
    }

    public String getUniqueIdentifierFromId(String id) {
        id = id.replace("StructureDefinition", "");
        StringBuilder uniqueIdentifier = new StringBuilder();
        String[] idTokens = id.split("-");
        for (String token : idTokens) {
            uniqueIdentifier.append(StringUtils.capitalize(token));
        }
        return StringUtils.capitalize(resolveSpecialCharacters(uniqueIdentifier.toString())).replaceAll("\\d", "");
    }

    /**
     * Resolve keyword conflicts of Ballerina
     *
     * @param keyword Ballerina keyword causes the conflict
     * @return replacement from tool configs
     */
    public String resolveKeywordConflict(String keyword) {
        if (this.KEYWORD_CONFLICTS_MAP.containsKey(keyword)) {
            return this.KEYWORD_CONFLICTS_MAP.get(keyword);
        } else if (this.toolConfig.getBallerinaKeywordConfig().containsKey(keyword)) {
            return this.toolConfig.getBallerinaKeywordConfig().get(keyword).getReplace();
        }
        return keyword;
    }

    public String resolveMultiDataTypeFieldNames(String fieldName, String typeName) {
        if (fieldName.endsWith("[x]")) {
            return resolveKeywordConflict(fieldName.substring(0, fieldName.length() - 3) +
                    StringUtils.capitalize(typeName));
        }
        return resolveKeywordConflict(fieldName);
    }

    /**
     * Resolve for special character
     *
     * @param specialChar special character
     * @return preferred string replacement
     */
    public String resolveSpecialCharacters(String specialChar) {
        if (SPECIAL_CHARACTERS_MAP.containsKey(specialChar))
            return SPECIAL_CHARACTERS_MAP.get(specialChar);
        return specialChar.replaceAll(Pattern.quote("[x]"), "")
                .replaceAll(Pattern.quote("/"), "")
                .replaceAll("-", "_")
                .replaceAll("\\s+", "");
    }

    /**
     * Checks whether certain datatype is allowed for Ballerina type inclusion.
     *
     * @param baseType Ballerina datatype name
     * @return is allowed for type inclusion or not
     */
    public boolean isTypeInclusion(String baseType) {
        return !NON_TYPE_INCLUSION_DATA_TYPES.contains(baseType);
    }

    /**
     * map datatype for provided field name
     *
     * @param fieldName provided field name
     * @return mapped datatype
     */

    public String mapToValueSetDatatype(String baseType, String fieldName, String assignedType) {
        if (VALUESET_DATA_TYPES.containsKey(baseType) && VALUESET_DATA_TYPES.get(baseType).containsKey(fieldName)) {
            return VALUESET_DATA_TYPES.get(baseType).get(fieldName);
        }
        return assignedType;
    }

    @SuppressWarnings("unchecked")
    public <T> T getTypedValue(String value, String datatype) {
        switch (datatype) {
            case "int":
                return (T) Integer.valueOf(value);
            case "boolean":
                return (T) Boolean.valueOf(value);
            default:
                return (T) String.format("\"%s\"", value);
        }
    }

    /**
     * Getter for newline in templates
     *
     * @return newline
     */
    public String getNewLine() {
        return "\n";
    }
}
