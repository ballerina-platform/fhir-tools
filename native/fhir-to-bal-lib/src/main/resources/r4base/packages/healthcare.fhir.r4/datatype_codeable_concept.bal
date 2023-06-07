// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A CodeableConcept represents a value that is usually supplied by providing a reference to one or more terminologies or
#  ontologies but may also be defined by the provision of text.
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + coding - Code defined by a terminology system  
# + text - Plain text representation of the concept
@DataTypeDefinition {
    name: "CodeableConcept",
    baseType: Element,
    elements: {
        "coding" : {
            name: "coding",
            dataType: Coding,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Code defined by a terminology system"
        },
        "text" : {
            name: "text",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Plain text representation of the concept"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    },
    validator:  validateFHIRDataType
}
public type CodeableConcept record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    Coding[] coding?;
    string text?;
|};
