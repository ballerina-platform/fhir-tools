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

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract function class for all Ballerina methods (Resource methods, static util, ...)
 */
public abstract class Function {

    private List<Parameter> inputParams = new ArrayList<>();
    private List<Parameter> outputParams = new ArrayList<>();
    private String signature;

    public List<Parameter> getInputParams() {
        return inputParams;
    }

    public void setInputParams(List<Parameter> inputParams) {
        this.inputParams = inputParams;
    }

    public List<Parameter> getOutputParams() {
        return outputParams;
    }

    public void setOutputParams(List<Parameter> outputParams) {
        this.outputParams = outputParams;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
