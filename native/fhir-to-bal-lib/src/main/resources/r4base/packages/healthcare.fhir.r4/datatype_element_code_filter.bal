// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# What codes are expected
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + path - A code-valued attribute to filter on
# + searchParam - A coded (token) parameter to search on  
# + valueSet - Valueset for the filter 
# + code - What code is expected
@DataTypeDefinition {
    name: "ElementCodeFilter",
    baseType: Element,
    elements: {
        "path": {
            name: "path",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "A code-valued attribute to filter on"
        },
        "searchParam": {
            name: "searchParam",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "A coded (token) parameter to search on"
        },
        "valueSet": {
            name: "valueSet",
            dataType: canonical,
            min: 0,
            max: 1,
            isArray: false,
            description: "Valueset for the filter"
        },
        "code": {
            name: "code",
            dataType: Coding,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "What code is expected"
        }

    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ElementCodeFilter record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    string path?;
    string searchParam?;
    canonical valueSet?;
    Coding[] code?;

|};
