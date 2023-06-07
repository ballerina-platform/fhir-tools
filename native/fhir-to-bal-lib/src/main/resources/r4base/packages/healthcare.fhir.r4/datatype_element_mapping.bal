// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Map element to another set of definitions
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + identity - Reference to mapping declaration  
# + language - Computable language of mapping 
# + 'map - Details of the mapping
# + comment - Comments about the mapping or its use
@DataTypeDefinition {
    name: "Range",
    baseType: Element,
    elements: {
        "identity": {
            name: "identity",
            dataType: id,
            min: 1,
            max: 1,
            isArray: false,
            description: "Reference to mapping declaration"
        },
        "language": {
            name: "language",
            dataType: code,
            min: 0,
            max: 1,
            isArray: false,
            description: "Computable language of mapping",
            valueSet: "https://hl7.org/fhir/valueset-mimetypes.html"
        },
        "map": {
            name: "map",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Details of the mapping"
        },
        "comment": {
            name: "comment",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Comments about the mapping or its use"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ElementMapping record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    id identity;
    code language?;
    string 'map;
    string comment?;
|};
