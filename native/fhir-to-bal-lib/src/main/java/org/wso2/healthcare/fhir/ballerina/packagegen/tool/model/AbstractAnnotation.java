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
