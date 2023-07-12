// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

public type Extension CodeableConceptExtension | ExtensionExtension | StringExtension | CodingExtension | CodeExtension | IntegerExtension;

# Every element in a resource or data type includes an optional "extension" child element that may be present
#  any number of times. 
#
# + url - identifies the meaning of the extension
# + extension - Additional content defined by implementations
@DataTypeDefinition {
    name: "Extension",
    baseType: Element,
    elements: {
        "url" : {
            name: "url",
            dataType: uri,
            min: 1,
            max: 1,
            isArray: false,
            description: "identifies the meaning of the extension"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type ExtensionExtension record {|
    *Element;

    uri url;
    Extension[] extension?;
|};

@DataTypeDefinition {
    name: "Extension",
    baseType: Element,
    elements: {
        "url" : {
            name: "url",
            dataType: uri,
            min: 1,
            max: 1,
            isArray: false,
            description: "identifies the meaning of the extension"
        },
        "valueString" : {
            name: "valueString",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Value of extension"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type StringExtension record {|
    uri url;
    string valueString;
|};

@DataTypeDefinition {
    name: "Extension",
    baseType: Element,
    elements: {
        "url" : {
            name: "url",
            dataType: uri,
            min: 1,
            max: 1,
            isArray: false,
            description: "identifies the meaning of the extension"
        },
        "valueCoding" : {
            name: "valueCoding",
            dataType: Coding,
            min: 0,
            max: 1,
            isArray: false,
            description: "Value of extension"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type CodingExtension record {|
    uri url;
    Coding valueCoding;
|};

public type CodeExtension record {|
    uri url;
    code valueCode;
|};

public type IntegerExtension record {|
    uri url;
    integer valueInteger;
|};

public type CodeableConceptExtension record {|
    uri url;
    CodeableConcept valueCodeableConcept;
|};
