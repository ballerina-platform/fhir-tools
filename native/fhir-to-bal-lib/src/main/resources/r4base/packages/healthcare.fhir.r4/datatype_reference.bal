// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A reference from one resource to another
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + reference - Literal reference, Relative, internal or absolute URL  
# * Rule : SHALL have a contained resource if a local reference is provided
# + 'type - Type the reference refers to (e.g. Patient) ResourceType(http://hl7.org/fhir/valueset-resource-types.html) (Extensible)  
# + identifier - Logical reference, when literal reference is not known  
# + display - Text alternative for the resource
@DataTypeDefinition {
    name: "Reference",
    baseType: Element,
    elements: {
        "reference": {
            name: "reference",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Literal reference, Relative, internal or absolute URL"
        },
        "type": {
            name: "type",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "Type the reference refers to (e.g. Patient)",
            valueSet: "http://hl7.org/fhir/valueset-resource-types.html"
        },
        "identifier": {
            name: "identifier",
            dataType: Identifier,
            min: 0,
            max: 1,
            isArray: false,
            description: "Logical reference, when literal reference is not known"
        },
        "display": {
            name: "display",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Text alternative for the resource"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type Reference record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    string reference?;
    string 'type?;
    Identifier identifier?;
    string display?;
|};
