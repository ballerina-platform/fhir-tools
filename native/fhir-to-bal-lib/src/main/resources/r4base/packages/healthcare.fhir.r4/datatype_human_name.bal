// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Description
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + use - usual | official | temp | nickname | anonymous | old | maiden
# + text - Text representation of the full name
# + family - Family name (often called 'Surname')
# + given - Given names (not always 'first'). Includes middle names. This repeating element order: Given Names appear in the correct order for presenting the name
# + prefix - Parts that come before the name. This repeating element order: Prefixes appear in the correct order for presenting the name
# + suffix - Parts that come after the name. This repeating element order: Suffixes appear in the correct order for presenting the name
# + period - Time period when name was/is in use
@DataTypeDefinition {
    name: "HumanName",
    baseType: Element,
    elements: {
        "use" : {
            name: "use",
            dataType: HumanNameUse,
            min: 0,
            max: 1,
            isArray: false,
            description: "usual | official | temp | nickname | anonymous | old | maiden"
        },
        "text" : {
            name: "text",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Text representation of the full name"
        },
        "family" : {
            name: "family",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Family name (often called 'Surname')"
        },
        "given" : {
            name: "given",
            dataType: string,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Given names (not always 'first'). Includes middle names. This repeating element order: Given Names appear in the correct order for presenting the name"
        },
        "prefix" : {
            name: "prefix",
            dataType: string,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Parts that come before the name. This repeating element order: Prefixes appear in the correct order for presenting the name"
        },
        "suffix" : {
            name: "suffix",
            dataType: string,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Parts that come after the name. This repeating element order: Suffixes appear in the correct order for presenting the name"
        },
        "period" : {
            name: "period",
            dataType: Period,
            min: 0,
            max: 1,
            isArray: false,
            description: "Time period when name was/is in use"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type HumanName record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    HumanNameUse use?;
    string text?;
    string family?;
    string[] given?;
    string[] prefix?;
    string[] suffix?;
    Period period?;
    
|};

public enum HumanNameUse {
    usual, official, temp, nickname, anonymous, old, maiden
}
