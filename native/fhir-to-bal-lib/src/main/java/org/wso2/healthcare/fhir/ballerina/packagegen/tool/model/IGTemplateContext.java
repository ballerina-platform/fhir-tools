// Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.model;

import java.util.Map;

/**
 * Class holder for IG related template context
 */
public class IGTemplateContext {
    private String title;
    private String igName;
    private String igCode;
    private String baseIgName;
    private Map<String, Map<String, SearchParameter>> searchParameters;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIgName() {
        return igName;
    }

    public void setIgName(String igName) {
        this.igName = igName;
    }

    public String getBaseIgName() {
        return baseIgName;
    }

    public void setBaseIgName(String baseIgName) {
        this.baseIgName = baseIgName;
    }

    public String getIgCode() {
        return igCode;
    }

    public void setIgCode(String igCode) {
        this.igCode = igCode;
    }

    public Map<String, Map<String, SearchParameter>> getSearchParameters() {
        return searchParameters;
    }

    public void setSearchParameters(Map<String, Map<String, SearchParameter>> searchParameters) {
        this.searchParameters = searchParameters;
    }
}
