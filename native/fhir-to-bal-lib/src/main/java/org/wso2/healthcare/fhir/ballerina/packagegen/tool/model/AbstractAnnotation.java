// Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
//
// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.model;

import java.util.HashMap;

/**
 * Abstracted model for Ballerina Annotations
 */
public abstract class AbstractAnnotation {
    private String name;
    private String baseType;
    private HashMap<String, AnnotationElement> elements;
    private ProcessingMetaInfo processingMetaInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseType() {
        return baseType;
    }

    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }

    public HashMap<String, AnnotationElement> getElements() {
        return elements;
    }

    public void setElements(HashMap<String, AnnotationElement> elements) {
        this.elements = elements;
    }

    public ProcessingMetaInfo getProcessingMetaInfo() {
        return processingMetaInfo;
    }

    public void setProcessingMetaInfo(ProcessingMetaInfo processingMetaInfo) {
        this.processingMetaInfo = processingMetaInfo;
    }
}
