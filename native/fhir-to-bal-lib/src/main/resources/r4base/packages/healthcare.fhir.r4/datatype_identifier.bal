// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A numeric or alphanumeric string that is associated with a single object or entity within a given system
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + use - usual | official | temp | secondary | old (If known) IdentifierUse (Required) (http://hl7.org/fhir/valueset-identifier-use.html) 
# + 'type - Description of identifier
# + system - The namespace for the identifier value
# + value - The value that is unique
# + period - Time period when id is/was valid for use
# + assigner - Organization that issued id (may be just text)
@DataTypeDefinition {
    name: "Identifier",
    baseType: Element,
    elements: {
        "use" : {
            name: "use",
            dataType: IdentifierUse,
            min: 0,
            max: 1,
            isArray: false,
            description: "usual | official | temp | secondary | old (If known) IdentifierUse (Required)"
        },
        "type" : {
            name: "type",
            dataType: CodeableConcept,
            min: 0,
            max: 1,
            isArray: false,
            description: "Description of identifier"
        },
        "system" : {
            name: "system",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "The namespace for the identifier value"
        },
        "value" : {
            name: "value",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "The value that is unique"
        },
        "period" : {
            name: "period",
            dataType: Period,
            min: 0,
            max: 1,
            isArray: false,
            description: "Time period when id is/was valid for use"
        },
        "assigner" : {
            name: "assigner",
            dataType: Reference,
            min: 0,
            max: 1,
            isArray: false,
            description: "Organization that issued id (may be just text)"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type Identifier record {|

    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    IdentifierUse use?;
    CodeableConcept 'type?;
    uri system?;
    string value?;
    Period period?;
    Reference assigner?;

|};

public enum IdentifierUse {
    usual, official, temp, secondary, old
}
