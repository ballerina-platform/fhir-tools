// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# The base definition for all elements contained inside a resource.
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
@DataTypeDefinition {
    name: "Element",
    baseType: (),
    elements: {
        "id" : {
            name: "id",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Unique id for inter-element referencing"
        },
        "extension" : {
            name: "extension",
            dataType: Extension,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Additional content defined by implementations"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type Element record {|
    string id?;
    Extension[] extension?;
    Element ...;
|};
