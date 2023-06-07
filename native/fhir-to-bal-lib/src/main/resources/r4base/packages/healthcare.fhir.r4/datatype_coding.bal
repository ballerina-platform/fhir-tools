// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A Coding is a representation of a defined concept using a symbol from a defined "code system"
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + system - Identity of the terminology system  
# + 'version - Version of the system - if relevant
# + code - Symbol in syntax defined by the system  
# + display - Representation defined by the system  
# + userSelected - If this coding was chosen directly by the user
@DataTypeDefinition {
    name: "Coding",
    baseType: Element,
    elements: {
        "system" : {
            name: "system",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "Identity of the terminology system"
        },
        "version" : {
            name: "version",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Version of the system - if relevant"
        },
        "code" : {
            name: "code",
            dataType: code,
            min: 0,
            max: 1,
            isArray: false,
            description: "Symbol in syntax defined by the system"
        },
        "display" : {
            name: "display",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Representation defined by the system"
        },
        "userSelected" : {
            name: "userSelected",
            dataType: boolean,
            min: 0,
            max: 1,
            isArray: false,
            description: "If this coding was chosen directly by the user"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type Coding record {|
    *Element;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    uri system?;
    string 'version?;
    code code?;
    string display?;
    boolean userSelected?;
|};
