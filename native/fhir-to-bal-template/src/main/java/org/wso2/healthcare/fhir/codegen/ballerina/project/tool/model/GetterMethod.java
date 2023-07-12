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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GetterMethod extends Function {

    private String fhirVersion;
    private List<String> initStatement;
    private List<String> checkStatement;
    private String returnStatement;

    public GetterMethod(String signature, String fhirVersion) {
        this.fhirVersion = fhirVersion;
        this.setSignature(signature);
        initStatement = new ArrayList<>();
        checkStatement = new ArrayList<>();
        returnStatement = "";
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public void addInitStatement(String initStatement) {
        this.initStatement.add(initStatement);
    }

    public void addCheckStatement(String checkStatement) {
        this.checkStatement.add(checkStatement);
    }


    public void setReturnStatement(String returnStatement) {
        this.returnStatement = returnStatement;
    }

    public String generateMethodBody() {
        String initStatements = StringUtils.join(initStatement,"\n");
        String checkStatements = StringUtils.join(checkStatement,"\n");
        return initStatements + "\n" + checkStatements + "\n" + returnStatement;
    }
}
