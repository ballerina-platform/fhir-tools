/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

/**
 * Holds FHIR interaction related business logic for each interaction(if specified any).
 */
public class FHIRInteractionMethods {

    private String searchMethodContent;
    private String readMethodContent;
    private String vReadMethodContent;
    private String createMethodContent;
    private String updateMethodContent;
    private String patchMethodContent;
    private String deleteMethodContent;
    private String historyMethodContent;

    public String getSearchMethodContent() {
        return searchMethodContent;
    }

    public void setSearchMethodContent(String searchMethodContent) {
        this.searchMethodContent = searchMethodContent;
    }

    public String getReadMethodContent() {
        return readMethodContent;
    }

    public void setReadMethodContent(String readMethodContent) {
        this.readMethodContent = readMethodContent;
    }

    public String getvReadMethodContent() {
        return vReadMethodContent;
    }

    public void setvReadMethodContent(String vReadMethodContent) {
        this.vReadMethodContent = vReadMethodContent;
    }

    public String getCreateMethodContent() {
        return createMethodContent;
    }

    public void setCreateMethodContent(String createMethodContent) {
        this.createMethodContent = createMethodContent;
    }

    public String getUpdateMethodContent() {
        return updateMethodContent;
    }

    public void setUpdateMethodContent(String updateMethodContent) {
        this.updateMethodContent = updateMethodContent;
    }

    public String getPatchMethodContent() {
        return patchMethodContent;
    }

    public void setPatchMethodContent(String patchMethodContent) {
        this.patchMethodContent = patchMethodContent;
    }

    public String getDeleteMethodContent() {
        return deleteMethodContent;
    }

    public void setDeleteMethodContent(String deleteMethodContent) {
        this.deleteMethodContent = deleteMethodContent;
    }

    public String getHistoryMethodContent() {
        return historyMethodContent;
    }

    public void setHistoryMethodContent(String historyMethodContent) {
        this.historyMethodContent = historyMethodContent;
    }

    public void setInteractionContentByType(String interactionType, String content) {
        switch (interactionType.toLowerCase()) {
            case "search":
                this.setSearchMethodContent(content);
                break;
            case "read":
                this.setReadMethodContent(content);
                break;
            case "vread":
                this.setvReadMethodContent(content);
                break;
            case "create":
                this.setCreateMethodContent(content);
                break;
            case "update":
                this.setUpdateMethodContent(content);
                break;
            case "delete":
                this.setDeleteMethodContent(content);
                break;
            case "patch":
                this.setPatchMethodContent(content);
                break;
            default:
                break;
        }
    }
}
