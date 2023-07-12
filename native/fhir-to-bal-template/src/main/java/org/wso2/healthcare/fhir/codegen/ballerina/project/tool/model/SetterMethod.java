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

public class SetterMethod extends Function {

    private boolean isContainer;
    private String fhirVersion;
    private String initStatement;
    private String checkStatement;
    private String returnStatement;

    public SetterMethod(String signature, String fhirVersion) {
        this.fhirVersion = fhirVersion;
        this.setSignature(signature);
        initStatement = "";
        checkStatement = "";
        returnStatement = "";
    }

    public boolean isContainer() {
        return isContainer;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public void setInitStatement(String initStatement) {
        this.initStatement = initStatement;
    }

    public void setCheckStatement(String checkStatement) {
        this.checkStatement = checkStatement;
    }


    public void setReturnStatement(String returnStatement) {
        this.returnStatement = returnStatement;
    }

    public String generateMethodBody() {
        return initStatement + "\n" + checkStatement + "\n" + returnStatement;
    }
}
