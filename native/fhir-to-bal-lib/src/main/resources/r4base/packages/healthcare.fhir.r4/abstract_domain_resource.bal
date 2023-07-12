// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A resource with narrative, extensions, and contained resources
#   * Rule: If the resource is contained in another resource, it SHALL NOT contain nested Resources
#   * Rule: If the resource is contained in another resource, it SHALL be referred to from elsewhere in the resource or SHALL refer to the containing resource
#   * Rule: If a resource is contained in another resource, it SHALL NOT have a meta.versionId or a meta.lastUpdated
#   * Rule: If a resource is contained in another resource, it SHALL NOT have a security label
#   * Guideline: A resource should have narrative for robust management
#
# + text - Text summary of the resource, for human interpretation
# + contained - Contained, inline Resources
# + extension - Additional content defined by implementations
# + modifierExtension - Extensions that cannot be ignored
@ResourceDefinition {
    resourceType: "DomainResource",
    baseType: Resource,
    profile: (),
    elements: {
        "text" : {
            name: "text",
            dataType: Narrative,
            min: 0,
            max: 1,
            isArray: false,
            description: "Text summary of the resource, for human interpretation"
        },
        "contained" : {
            name: "contained",
            dataType: Resource,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Contained, inline Resources"
        },
        "extension" : {
            name: "extension",
            dataType: Extension,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Additional content defined by implementations"
        },
        "modifierExtension" : {
            name: "modifierExtension",
            dataType: Extension,
            min: 0,
            max: int:MAX_VALUE,
            isArray: false,
            description: "Extensions that cannot be ignored"
        }
    },
    serializers: {
        'xml: fhirResourceXMLSerializer, 
        'json: fhirResourceJsonSerializer
    }
}
public type DomainResource record {|
    *Resource;
    Narrative text?;
    Resource[] contained?;
    Extension[] extension?;
    Extension[] modifierExtension?;
    Element ...;
|};
