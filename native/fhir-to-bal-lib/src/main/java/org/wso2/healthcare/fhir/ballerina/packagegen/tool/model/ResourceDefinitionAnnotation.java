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
import java.util.Map;

/**
 * Extended definition of Ballerina Annotation for Resource Definition Annotations
 */
public class ResourceDefinitionAnnotation extends AbstractAnnotation {
    private String profile;
    private Map<String, String> resourceSerializers;
    private String resourceTypeValidator;

    public ResourceDefinitionAnnotation() {
        this.resourceSerializers = new HashMap<>();
        this.resourceSerializers.put("'xml", "fhirResourceXMLSerializer");
        this.resourceSerializers.put("'json", "fhirResourceJsonSerializer");
        this.resourceTypeValidator = "validateFHIRResource";
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public Map<String, String> getResourceSerializers() {
        return resourceSerializers;
    }

    public void setResourceSerializers(HashMap<String, String> resourceSerializers) {
        this.resourceSerializers = resourceSerializers;
    }

    public String getResourceTypeValidator() {
        return resourceTypeValidator;
    }

    public void setResourceTypeValidator(String resourceTypeValidator) {
        this.resourceTypeValidator = resourceTypeValidator;
    }
}
