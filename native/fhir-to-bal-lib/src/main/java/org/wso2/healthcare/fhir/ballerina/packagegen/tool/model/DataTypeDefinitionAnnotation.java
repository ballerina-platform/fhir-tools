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
 * Extended definition of Ballerina Annotation for Data Type Annotations
 */
public class DataTypeDefinitionAnnotation extends AbstractAnnotation {
    private HashMap<String, String> dataSerializers;

    public DataTypeDefinitionAnnotation() {
        this.dataSerializers = new HashMap<>();
        this.dataSerializers.put("'xml", "complexDataTypeXMLSerializer");
        this.dataSerializers.put("'json", "complexDataTypeJsonSerializer");
    }

    public HashMap<String, String> getDataSerializers() {
        return dataSerializers;
    }

    public void setDataSerializers(HashMap<String, String> dataSerializers) {
        this.dataSerializers = dataSerializers;
    }

    @Override
    public String getBaseType() {
        return "()";
    }

    @Override
    public void setBaseType(String baseType) {
        super.setBaseType(baseType);
    }
}
