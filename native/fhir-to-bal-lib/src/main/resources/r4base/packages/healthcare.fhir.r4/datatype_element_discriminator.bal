// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Element values that are used to distinguish the slices
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + 'type - value | exists | pattern | type | profile  
# + path - Path to element value
@DataTypeDefinition {
    name: "ElementDiscriminator",
    baseType: Element,
    elements: {
        "type": {
            name: "type",
            dataType: ElementDiscriminatorType,
            min: 1,
            max: 1,
            isArray: false,
            description: "value | exists | pattern | type | profile",
            valueSet: "https://hl7.org/fhir/valueset-discriminator-type.html"
        },
        "path": {
            name: "path",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Path to element value"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ElementDiscriminator record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    ElementDiscriminatorType 'type;
    string path;
|};

public enum ElementDiscriminatorType {
    value,
    exists,
    pattern,
    'type,
    profile

}
