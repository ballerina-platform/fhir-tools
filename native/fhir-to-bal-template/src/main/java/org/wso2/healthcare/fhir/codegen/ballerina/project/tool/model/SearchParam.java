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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchParam {

    private final String name;
    private final String code;
    private String targetResource;
    private String description;
    private String documentation;
    private boolean builtIn;
    private String type;

    public SearchParam(String name, String code) {
        this.name = name;
        this.code = code;
        this.builtIn = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return extractDescription(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public String getTargetResource() {
        return targetResource;
    }

    public void setTargetResource(String targetResource) {
        this.targetResource = targetResource;
    }

    private String extractDescription(String originalDescription) {
        List<String> descriptions;
        if (originalDescription.contains("Multiple Resources:")) {
            descriptions = Arrays.asList(originalDescription.split("\\r\n\\* "));
            for (String resourceDesc : descriptions
            ) {
                if (resourceDesc.contains(targetResource)) {
                    return resourceDesc;
                }
            }
        } else {
            originalDescription = originalDescription.replace("\n", "");
            originalDescription = originalDescription.replace("\r", "");
            return originalDescription;
        }
        return originalDescription;
    }

    public String getCode() {
        return code;
    }

    /**
     * @param type the definition's {@code SearchParameter.type} code, e.g. {@code token}
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * The type as a {@code FHIRSearchParameterType} constant name ({@code TOKEN}, {@code DATE}, ...).
     *
     * <p>Declaring it matters at runtime, not just in the CapabilityStatement: the FHIR service registers any
     * parameter its base registry does not know (every IG-defined one) with this type, and as STRING when it is
     * absent, which mis-parses token, date and reference searches.
     *
     * @return the constant name, or null when the definition has no type or one the Ballerina FHIR libraries do
     *         not model (R5's {@code resource})
     */
    public String getTypeConstant() {
        return type != null && SEARCH_PARAMETER_TYPES.contains(type) ? type.toUpperCase(Locale.ROOT) : null;
    }

    private static final Set<String> SEARCH_PARAMETER_TYPES = Set.of(
            "number", "date", "string", "token", "reference", "composite", "quantity", "uri", "special");
}
