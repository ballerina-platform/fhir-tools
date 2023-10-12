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

public class ResourceMethod extends Function {
    private final String type;
    private final String resourceName;
    private String contextInformation;
    private final String resourceContext;
    private final String httpMethod;
    private ArrayList<String> methodParams;
    private String returnType;
    private String descriptionComment;

    public ResourceMethod(String type, String resourceName, ArrayList<String> methodParams, String httpMethod, String returnType, String descriptionComment) {
        this.type = type;
        this.resourceName = resourceName;
        this.methodParams = methodParams;
        this.resourceContext = resolveContext();
        this.httpMethod = httpMethod;
        this.returnType = returnType;
        this.descriptionComment = descriptionComment;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getContextInformation() {
        return contextInformation;
    }

    public void setContextInformation(String contextInformation) {
        this.contextInformation = contextInformation;
    }

    public String getDescriptionComment() {
        return descriptionComment;
    }

    public void setDescriptionComment(String descriptionComment) {
        this.descriptionComment = descriptionComment;
    }

    public String getType() {
        return type;
    }

    public String getResourceContext() {
        return resourceContext;
    }

    public String setResourceContext(String resourceContext) {
        return this.resourceContext;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public ArrayList<String> getMethodParams() {
        return methodParams;
    }

    public void setMethodParams(ArrayList<String> methodParams) {
        this.methodParams = methodParams;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    private String resolveContext() {
        String context = this.resourceName;
        switch (this.type) {
            case "read":
            case "update":
            case "patch":
            case "delete": {
                context = context + "/[string id]";
                break;
            }
            case "vread": {
                context = context + "/[string id]/_history/[string vid]";
                break;
            }
            case "search":
            case "create": {
                break;
            }
            case "history-instance": {
                context = context + "/[string id]/_history";
                break;
            }
            case "history-type": {
                context = context + "/_history";
                break;
            }
        }
        return context;
    }
}
