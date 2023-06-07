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
