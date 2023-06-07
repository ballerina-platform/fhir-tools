// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A measured or measurable amount
# Rule: If a code for the unit is present, the system SHALL also be present
# Elements defined in Ancestors: id, extension
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + value - Numerical value (with implicit precision)  
# + comparator - < | <= | >= | > - how to understand the value 
# + unit - Unit representation 
# + system - System that defines coded unit form 
# + code - Coded form of the unit
@DataTypeDefinition {
    name: "Quantity",
    baseType: Element,
    elements: {
        "value": {
            name: "value",
            dataType: decimal,
            min: 0,
            max: 1,
            isArray: false,
            description: "Numerical value (with implicit precision)"
        },
        "comparator": {
            name: "comparator",
            dataType: QuantityComparatorCode,
            min: 0,
            max: 1,
            isArray: false,
            description: "< | <= | >= | > - how to understand the value",
            valueSet: "http://hl7.org/fhir/ValueSet/quantity-comparator"
        },
        "unit": {
            name: "unit",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Unit representation"
        },
        "system": {
            name: "system",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "System that defines coded unit form"
        },
        "code": {
            name: "code",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Coded form of the unit"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type Quantity record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    decimal value?;
    QuantityComparatorCode comparator?;
    string unit?;
    uri system?;
    code code?;
|};

public type SimpleQuantity record {|
    *Quantity;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    decimal value?;
    string unit?;
    uri system?;
    code code?;
|};

public type Age record {|
    *Quantity;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    integer ageValue?;
    string unit?;
    uri system?;
    code code?;

|};

public type Distance record {|
    *Quantity;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    decimal distanceValue?;
    string unit?;
    uri system?;
    code code?;
|};

public type Duration record {|
    *Quantity;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    decimal durationValue?;
    string unit?;
    uri system?;
    code code?;
|};

public type Count record {|
    *Quantity;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    integer countValue?;
    string unit?;
    uri system?;
    code code?;
|};

public type MoneyQuantity record {|
    *Quantity;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    decimal moneyValue?;
    string unit?;
    uri system?;
    code code?;
|};

public enum QuantityComparatorCode {
    LESS_THAN = "<",
    LESS_THAN_EQUAL = "<=",
    GREATER_THAN_EQUAL = ">=",
    GREATER_THAN = ">"
};
