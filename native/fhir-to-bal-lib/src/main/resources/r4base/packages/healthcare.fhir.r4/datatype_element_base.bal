// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Base definition information for tools
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + path - Path that identifies the base element  
# + min - Min cardinality of the base element 
# + max - Max cardinality of the base element
@DataTypeDefinition {
    name: "ElementBase",
    baseType: Element,
    elements: {
        "path": {
            name: "path",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Path that identifies the base element"
        },
        "min": {
            name: "min",
            dataType: unsignedInt,
            min: 1,
            max: 1,
            isArray: false,
            description: "Min cardinality of the base element"
        },
        "max": {
            name: "max",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Max cardinality of the base element"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ElementBase record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    string path;
    unsignedInt min;
    string max;
|};
