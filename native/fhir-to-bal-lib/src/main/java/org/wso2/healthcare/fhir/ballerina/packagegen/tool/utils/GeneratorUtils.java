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

import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.SearchParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GeneratorUtils {
    private final static HashMap<String, String> DATA_TYPE_MAP = new HashMap<String, String>() {{
        put("http://hl7.org/fhirpath/System.String", "string");
        put("http://hl7.org/fhirpath/System.Date", "date");
        put("url", "urlType");
    }};

    /**
     * Resolve for specific data types with preferred type from config.
     *
     * @param fhirDataType data type identifier from FHIR structure definition
     * @return preferred replacement from config
     */
    public static String resolveDataType(BallerinaPackageGenToolConfig toolConfig, String fhirDataType) {
        if (DATA_TYPE_MAP.containsKey(fhirDataType)) {
            return DATA_TYPE_MAP.get(fhirDataType);
        } else if (toolConfig.getDataTypeMappingConfigs().containsKey(fhirDataType))
            return toolConfig.getDataTypeMappingConfigs().get(fhirDataType).getBallerinaType();
        else
            return fhirDataType;
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
}
