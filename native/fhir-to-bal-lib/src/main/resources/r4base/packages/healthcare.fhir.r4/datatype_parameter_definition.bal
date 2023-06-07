// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Definition of a parameter to a module
#
# + id - Field Description  
# + extension - Field Description  
# + name - Name used to access the parameter value  
# + use - in | out  
# + min - Minimum cardinality  
# + max - Maximum cardinality (a number of *)  
# + documentation - A brief description of the parameter 
# + 'type - What type of value  
# + profile - What profile the value is expected to be
@DataTypeDefinition {
    name: "ParameterDefinition",
    baseType: Element,
    elements: {
        "name": {
            name: "name",
            dataType: code,
            min: 0,
            max: 1,
            isArray: false,
            description: "Name used to access the parameter value"
        },
        "use": {
            name: "use",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "in | out",
            valueSet: "https://hl7.org/fhir/valueset-operation-parameter-use.html"
        },
        "min": {
            name: "min",
            dataType: integer,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum cardinality"
        },
        "max": {
            name: "max",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum cardinality (a number of *)"
        },
        "documentation": {
            name: "documentation",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "A brief description of the parameter"
        },
        "type": {
            name: "type",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "What type of value",
            valueSet: "https://hl7.org/fhir/valueset-all-types.html"
        },
        "profile": {
            name: "profile",
            dataType: canonical,
            min: 0,
            max: 1,
            isArray: false,
            description: "What profile the value is expected to be"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ParameterDefinition record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    code name?;
    ParameterDefinitionUse use;
    integer min?;
    string max?;
    string documentation?;
    code 'type;
    canonical profile?;

|};

public enum ParameterDefinitionUse {
    'in,
    out
}
