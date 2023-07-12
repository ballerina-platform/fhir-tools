// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# ValueSet details if this is coded
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + strength - required | extensible | preferred | example  
# + description - Human explanation of the value set
# + valueSet - Source of value set
@DataTypeDefinition {
    name: "ElementBinding",
    baseType: Element,
    elements: {
        "strength": {
            name: "strength",
            dataType: StrengthCode,
            min: 1,
            max: 1,
            isArray: false,
            description: "required | extensible | preferred | example",
            valueSet: "https://hl7.org/fhir/valueset-binding-strength.html"
        },
        "description": {
            name: "description",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Human explanation of the value set"
        },
        "valueSet": {
            name: "valueSet",
            dataType: canonical,
            min: 0,
            max: 1,
            isArray: false,
            description: "Source of value set"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ElementBinding record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    StrengthCode strength;
    string description?;
    canonical valueSet?;
|};

public enum StrengthCode {
    required,
    extensible,
    preferred,
    example
}
