// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Base for elements defined inside a resource Elements defined in Ancestors: id, extension
#
# + id - Unique id for inter-element referencing  
# + extension - Additional content defined by implementations  
# + modifierExtension - Extensions that cannot be ignored even if unrecognized
@DataTypeDefinition {
    name: "BackboneElement",
    baseType: Element,
    elements: {
        " modifierExtension": {
            name: " modifierExtension",
            dataType: Extension,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Extensions that cannot be ignored even if unrecognized"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}

public type BackboneElement record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    Extension[] modifierExtension?;
|};
