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

public class ResourceMethod extends Function {
    private final String type;
    private String contextInformation;
    private String resourceContext;
    private String httpMethod;
    private String descriptionComment;

    public ResourceMethod(String type, String context, String httpMethod) {
        this.type = type;
        this.resourceContext = context;
        this.httpMethod = httpMethod;
        this.descriptionComment = "";
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

    public String getHttpMethod() {
        return httpMethod;
    }
}
