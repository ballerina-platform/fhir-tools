/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import org.hl7.fhir.r4.model.SearchParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchParam {

    private final String name;
    private final String code;
    private String targetResource;
    private SearchParameter searchParamDef;
    private String description;
    private String documentation;
    private boolean builtIn;

    public SearchParam(String name, String code) {
        this.name = name;
        this.code = code;
        this.builtIn = false;
    }

    public String getName() {
        return name;
    }

    public SearchParameter getSearchParamDef() {
        return searchParamDef;
    }

    public void setSearchParamDef(SearchParameter searchParamDef) {
        this.searchParamDef = searchParamDef;
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

    private String extractDescription(String originalDescription){
        List<String> descriptions;
        if (originalDescription.contains("Multiple Resources:")){
             descriptions =  Arrays.asList(originalDescription.split("\\r\n\\* "));
            for (String resourceDesc:descriptions
                 ) {
                if (resourceDesc.contains(targetResource)){
                    return resourceDesc;
                }
            }
        }else{
            return originalDescription.replace("\n", "");
        }
        return originalDescription;
    }

    public String getCode() {
        return code;
    }
}
