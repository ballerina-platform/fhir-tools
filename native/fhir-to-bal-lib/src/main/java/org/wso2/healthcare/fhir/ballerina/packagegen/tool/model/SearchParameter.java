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

import java.util.ArrayList;

/**
 * Model for FHIR search parameter
 */
public class SearchParameter {
    private String name;
    private String type;
    private ArrayList<String> base;
    private String expression;

    public SearchParameter() {
        this.base = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<String> getBase() {
        return base;
    }

    public void setBase(ArrayList<String> base) {
        this.base = base;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
