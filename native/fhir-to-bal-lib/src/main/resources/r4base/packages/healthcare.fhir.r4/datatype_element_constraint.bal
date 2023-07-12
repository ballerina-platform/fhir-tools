// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Condition that must evaluate to true
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + key - Target of 'condition' reference above 
# + requirements - Why this constraint is necessary or appropriate
# + severity - error | warning
# + human - Human description of constraint  
# + expression - FHIRPath expression of constraint  
# + xpath - XPath expression of constraint
# + 'source - Reference to original source of constraint
@DataTypeDefinition {
    name: "ElementConstraint",
    baseType: Element,
    elements: {
        "key": {
            name: "key",
            dataType: id,
            min: 1,
            max: 1,
            isArray: false,
            description: "Target of 'condition' reference above"
        },
        "requirements": {
            name: "requirements",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Why this constraint is necessary or appropriate"
        },
        "severity": {
            name: "severity",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "error | warning",
            valueSet: "https://hl7.org/fhir/valueset-constraint-severity.html"
        },
        "human": {
            name: "human",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Human description of constraint"
        },
        "expression": {
            name: "expression",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "FHIRPath expression of constraint"
        },
        "xpath": {
            name: "xpath",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "XPath expression of constraint"
        },
        "source": {
            name: "source",
            dataType: canonical,
            min: 0,
            max: 1,
            isArray: false,
            description: "Reference to original source of constraint"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ElementConstraint record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    id key;
    string requirements?;
    string severity;
    string human;
    string expression?;
    string xpath?;
    canonical 'source?;
|};
