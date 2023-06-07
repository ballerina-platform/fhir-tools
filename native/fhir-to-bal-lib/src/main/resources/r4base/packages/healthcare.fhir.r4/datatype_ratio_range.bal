// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# DesRange of ratio values
# Rule: One of lowNumerator or highNumerator and denominator SHALL be present, or all are absent. 
# If all are absent, there SHALL be some extension present
# Rule: If present, lowNumerator SHALL have a lower value than highNumerator Elements 
# defined in Ancestors: id, extensioncription
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + lowNumerator - Low Numerator value 
# + highNumerator - High Numerator value
# + denominator - Denominator value
@DataTypeDefinition {
    name: "RangeRatio",
    baseType: Element,
    elements: {
        "lowNumerator": {
            name: "lowNumerator",
            dataType: SimpleQuantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Low Numerator value"
        },
        "highNumerator": {
            name: "highNumerator",
            dataType: SimpleQuantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "High Numerator value"
        },
        "denominator": {
            name: "denominator",
            dataType: SimpleQuantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Denominator value"
        }

    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type RatioRange record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    SimpleQuantity highNumerator?;
    SimpleQuantity lowNumerator?;
    SimpleQuantity denominator?;
|};
