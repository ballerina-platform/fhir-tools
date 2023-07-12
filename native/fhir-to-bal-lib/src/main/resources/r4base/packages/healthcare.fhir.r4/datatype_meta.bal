// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Each resource contains an element "meta", of type "Meta", which is a set of metadata that provides technical and
#  workflow context to the resource.
# 
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + versionId - Version specific identifier
# + lastUpdated - When the resource version last changed
# + 'source - Identifies where the resource comes from  
# + profile - Profiles this resource claims to conform to  
# + security - Security Labels applied to this resource SecurityLabels(http://hl7.org/fhir/valueset-security-labels.html) (Extensible)
# + tag - Tags applied to this resource Common Tags(http://hl7.org/fhir/valueset-common-tags.html) (Example)
@DataTypeDefinition {
    name: "Meta",
    baseType: Element,
    elements: {
        "versionId": {
            name: "versionId",
            dataType: id,
            min: 0,
            max: 1,
            isArray: false,
            description: "Version specific identifier"
        },
        "lastUpdated": {
            name: "lastUpdated",
            dataType: instant,
            min: 0,
            max: 1,
            isArray: false,
            description: "When the resource version last changed"
        },
        "source": {
            name: "source",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "Identifies where the resource comes from"
        },
        "profile": {
            name: "profile",
            dataType: canonical,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Profiles this resource claims to conform to"
        },
        "security": {
            name: "security",
            dataType: string,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Security Labels applied to this resource SecurityLabels(http://hl7.org/fhir/valueset-security-labels.html) (Extensible)"
        },
        "tag": {
            name: "tag",
            dataType: Coding,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Tags applied to this resource Common Tags(http://hl7.org/fhir/valueset-common-tags.html) (Example)"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type Meta record {|
    *Element;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    id versionId?;
    instant lastUpdated?;
    uri 'source?;
    canonical[] profile?;
    Coding[] security?;
    Coding[] tag?;
|};
