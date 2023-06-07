// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# This element is sliced - slices follow
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + discriminator - Element values that are used to distinguish the slices 
# + description - Text description of how slicing works (or not)
# + ordered - If elements must be in same order as slices 
# + rules - closed | open | openAtEnd
@DataTypeDefinition {
    name: "ElementSlicing",
    baseType: Element,
    elements: {
        "discriminator": {
            name: "discriminator",
            dataType: ElementDiscriminator,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Element values that are used to distinguish the slices"
        },
        "description": {
            name: "description",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Text description of how slicing works (or not)"
        },
        "ordered": {
            name: "ordered",
            dataType: boolean,
            min: 0,
            max: 1,
            isArray: false,
            description: "If elements must be in same order as slices"
        },
        "rules": {
            name: "rules",
            dataType: ElementSlicingRules,
            min: 1,
            max: 1,
            isArray: false,
            description: "closed | open | openAtEnd",
            valueSet: "https://hl7.org/fhir/valueset-resource-slicing-rules.html"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ElementSlicing record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    ElementDiscriminator[] discriminator?;
    string description?;
    boolean ordered?;
    ElementSlicingRules rules;

|};

public enum ElementSlicingRules {
    closed,
    open,
    openAtEnd
}
