// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Base Resource
#
# + resourceType - Type of resource
# + id - Logical id of this artifact 
# + meta - Metadata about the resource
# + implicitRules - A set of rules under which this content was created
# + language - Language of the resource content Common Languages(http://hl7.org/fhir/valueset-languages.html) (Preferred but limited to AllLanguages)
@ResourceDefinition {
    resourceType: "Resource",
    profile: (),
    baseType: (),
    elements: {
        "id" : {
            name: "id",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Logical id of this artifact"
        },
        "meta" : {
            name: "meta",
            dataType: Meta,
            min: 0,
            max: 1,
            isArray: false,
            description: "Metadata about the resource"
        },
        "implicitRules" : {
            name: "implicitRules",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "A set of rules under which this content was created"
        },
        "language" : {
            name: "language",
            dataType: code,
            min: 0,
            max: 1,
            isArray: false,
            description: "Language of the resource content Common Languages(http://hl7.org/fhir/valueset-languages.html) (Preferred but limited to AllLanguages)"
        }
    },
    serializers: {
        'xml: fhirResourceXMLSerializer, 
        'json: fhirResourceJsonSerializer
    }
}
public type Resource record {|
    string resourceType;
    string id?;
    Meta meta?;
    uri implicitRules?;
    code language?;
|};
