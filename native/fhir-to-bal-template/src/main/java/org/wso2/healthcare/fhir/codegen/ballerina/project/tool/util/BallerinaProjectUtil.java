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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.util;

import org.apache.commons.text.CaseUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for Ballerina project generator.
 */
public class BallerinaProjectUtil {

    /**
     * Aggregate substrings to a camelcase string.
     *
     * @param substrings list of substrings
     * @return aggregated string
     */
    public static String aggregateCamelcase(List<String> substrings) {

        String aggregatedName = substrings.get(0).toLowerCase();
        if (substrings.size() == 1) {
            return aggregatedName;
        }

        for (int i = 1; i < substrings.size(); i++) {
            String substring = substrings.get(i);
            aggregatedName = String.format("%s%s", aggregatedName, CaseUtils.toCamelCase(substring, true, '_'));
        }
        return aggregatedName;
    }

    /**
     * Resolve for special character
     *
     * @param specialChar special character
     * @return preferred string replacement
     */
    public static String resolveSpecialCharacters(String specialChar) {
        return specialChar.replaceAll(Pattern.quote("[x]"), "")
                .replaceAll(Pattern.quote("/"), "")
                .replaceAll("-", "_")
                .replaceAll("\\s+", "");
    }
}
