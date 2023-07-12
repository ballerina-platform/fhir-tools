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

import org.apache.commons.lang3.StringUtils;

public class Parameter {

    private final String identifier;
    private final String ballerinaPackage;
    private final String type;

    public Parameter(String identifier, String ballerinaPackage, String type) {
        this.identifier = identifier;
        this.ballerinaPackage = ballerinaPackage;
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getBallerinaPackage() {
        return ballerinaPackage;
    }

    public String getType() {
        return type;
    }

    public String toString() {

        if (StringUtils.isEmpty(identifier) && StringUtils.isEmpty(ballerinaPackage)) {
            //return param - generic type
            return type;
        } else if (StringUtils.isEmpty(identifier) && !StringUtils.isEmpty(ballerinaPackage)) {
            //return param - defined type
            return ballerinaPackage + ":" + type;
        } else if (!StringUtils.isEmpty(identifier) && StringUtils.isEmpty(ballerinaPackage)) {
            //input param - generic type
            return type + " " + identifier;
        }else {
            return ballerinaPackage + ":" + type + " " + identifier;
        }
    }
}
