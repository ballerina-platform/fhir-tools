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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.DataTypesRegistry;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.AnnotationElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.BallerinaDataType;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DataTypeDefinitionAnnotation;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ExtendedElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.SearchParameter;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.ResourceContextGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GeneratorUtils {

    private static final Log LOG = LogFactory.getLog(GeneratorUtils.class);
    private final static HashMap<String, String> DATA_TYPE_MAP = new HashMap<String, String>() {{
        put("http://hl7.org/fhirpath/System.String", "string");
        put("http://hl7.org/fhirpath/System.Date", "date");
        put("url", "urlType");
    }};

    /**
     * Check whether the data type should be replaced by a ballerina type.
     * @param dataType data type identifier from FHIR structure definition
     * @return true if the data type should be replaced by a ballerina type
     */
    public static boolean shouldReplacedByBalType(String dataType) {
        return DATA_TYPE_MAP.containsKey(dataType);
    }

    public static String getBalTypeWithImport(String dataType) {
        return "string".equals(dataType) || "boolean".equals(dataType) ? dataType : "r4:" + dataType;
    }

    /**
     * Resolve for specific data types with preferred type from config.
     *
     * @param fhirDataType data type identifier from FHIR structure definition
     * @return preferred replacement from config
     */
    public static String resolveDataType(BallerinaPackageGenToolConfig toolConfig, String fhirDataType) {
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
    public static String resolveDataType(String fhirDataType) {
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
    public static AnnotationElement populateAnnotationElement(Element element) {
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
    public static ExtendedElement populateExtendedElement(Element element, BallerinaDataType balType, String baseType,
                                                    String typeNamePrefix) {
        LOG.debug("Started: Resource Extended Element population");
        ExtendedElement extendedElement = new ExtendedElement();
        String extendedElementTypeName = GeneratorUtils.generateExtendedElementIdentifier(element, typeNamePrefix);
        extendedElement.setTypeName(extendedElementTypeName);
        element.setDataType(extendedElementTypeName);
        extendedElement.setBalDataType(balType);
        extendedElement.setBaseType(baseType);
        if (element.getChildElements() != null) {
            extendedElement.setElements(element.getChildElements());
        }
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
    public static String generateExtendedElementIdentifier(Element element, String prefix) {
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

        DataTypesRegistry.getInstance().addDataType(suggestedIdentifier.toString());
        LOG.debug("Ended: Extended Element Identifier generation");
        return suggestedIdentifier.toString();
    }
}
