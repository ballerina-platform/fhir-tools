// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Related artifacts for a knowledge resource
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + 'type - documentation | justification | citation | predecessor | successor | derived-from | depends-on | composed-of
# + label - Short label  
# + display - Brief description of the related artifact 
# + citation - Bibliographic citation for the artifact 
# + url - Where the artifact can be accessed 
# + document - What document is being referenced
# + 'resource - What resource is being referenced
@DataTypeDefinition {
    name: "RelatedArtifact",
    baseType: Element,
    elements: {
        "type": {
            name: "type",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "documentation | justification | citation | predecessor | successor | derived-from | depends-on |" +
            "composed-of",
            valueSet: "https://hl7.org/fhir/valueset-related-artifact-type.html"
        },
        "label": {
            name: "label",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Short label"
        },
        "display": {
            name: "display",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Brief description of the related artifact"
        },
        "citation": {
            name: "citation",
            dataType: markdown,
            min: 0,
            max: 1,
            isArray: false,
            description: "Bibliographic citation for the artifact"
        },
        "url": {
            name: "url",
            dataType: urlType,
            min: 0,
            max: 1,
            isArray: false,
            description: "Where the artifact can be accessed"
        },
        "document": {
            name: "document",
            dataType: Attachment,
            min: 0,
            max: 1,
            isArray: false,
            description: "What document is being referenced"
        },
        "resource": {
            name: "resource",
            dataType: canonical,
            min: 0,
            max: 1,
            isArray: false,
            description: "What resource is being referenced"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type RelatedArtifact record {|
    *Element;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    code 'type;
    string label?;
    string display?;
    markdown citation?;
    urlType url?;
    Attachment document?;
    canonical 'resource?;

|};
