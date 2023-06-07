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
