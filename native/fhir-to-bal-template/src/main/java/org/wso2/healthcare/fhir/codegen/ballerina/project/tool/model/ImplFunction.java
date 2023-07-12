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

public class ImplFunction extends Function{

    private final String interaction;
    private final String fhirVersion;
    private final Function sourceConnectFunction;

    public ImplFunction(String interaction, String signature, String fhirVersion) {
        this.interaction = interaction;
        this.fhirVersion = fhirVersion;
        this.setSignature(signature);
        sourceConnectFunction = new SourceConnectFunction(interaction);
    }

    public String getInteraction() {
        return interaction;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public Function getSourceConnectFunction() {
        return sourceConnectFunction;
    }

    private class SourceConnectFunction extends Function{

        private String resourceType;

        public SourceConnectFunction(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getResourceType() {
            return resourceType;
        }
    }


}
