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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.versions.r5;

import org.hl7.fhir.r5.model.ElementDefinition;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.util.HashMap;
import java.util.regex.Pattern;

public class R5GeneratorUtils extends GeneratorUtils {
    private static final R5GeneratorUtils instance = new R5GeneratorUtils();

    public static R5GeneratorUtils getInstance() {
        return instance;
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
}
