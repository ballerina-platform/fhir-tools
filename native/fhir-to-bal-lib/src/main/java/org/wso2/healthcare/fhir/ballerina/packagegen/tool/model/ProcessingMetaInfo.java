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

/**
 * Model for ProcessingMetaInfo metadata in Ballerina Annotations
 */
public class ProcessingMetaInfo {
    private String targetModel;
    private Relocation[] relocations;

    public String getTargetModel() {
        return targetModel;
    }

    public void setTargetModel(String targetModel) {
        this.targetModel = targetModel;
    }

    public Relocation[] getRelocations() {
        return relocations;
    }

    public void setRelocations(Relocation[] relocations) {
        this.relocations = relocations;
    }
}
