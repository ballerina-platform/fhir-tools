// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# An expression that can be used to generate a value
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + description - Natural language description of the condition  
# + name - Short name assigned to expression for reuse 
# + language - text/cql | text/fhirpath | application/x-fhir-query | text/cql-identifier | text/cql-expression | etc.
# + expression - Expression in specified language  
# + reference - Where the expression is found
@DataTypeDefinition {
    name: "Expression",
    baseType: Element,
    elements: {
        "description": {
            name: "description",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Natural language description of the condition"
        },
        "name": {
            name: "name",
            dataType: id,
            min: 0,
            max: 1,
            isArray: false,
            description: "Short name assigned to expression for reuse"
        },
        "language": {
            name: "language",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "text/cql | text/fhirpath | application/x-fhir-query | text/cql-identifier | text/cql-expression | etc.",
            valueSet: "https://hl7.org/fhir/valueset-expression-language.html"
        },
        "expression": {
            name: "expression",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Expression in specified language"
        },
        "reference": {
            name: "reference",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "Where the expression is found"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}

public type Expression record {
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    string description?;
    id name?;
    code language;
    string expression?;
    uri reference?;

};
