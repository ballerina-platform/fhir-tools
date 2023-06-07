// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# An amount of economic utility in some recognized currency.
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + value - Numerical value (with implicit precision)
# + currency - ISO 4217 Currency Code (Required) (https://hl7.org/fhir/valueset-currencies.html)
@DataTypeDefinition {
    name: "Money",
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
        "currency": {
            name: "currency",
            dataType: code,
            min: 0,
            max: 1,
            isArray: false,
            description: "ISO 4217 Currency Code",
            valueSet: "https://hl7.org/fhir/valueset-currencies.html"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}

public type Money record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    decimal value?;
    code currency?;

|};
