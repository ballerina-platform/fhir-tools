// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Contributor information Elements defined in Ancestors: id, extension
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + 'type - author | editor | reviewer | endorser  ContributorType (Required)
# + name - Who contributed the content  
# + contact - Contact details of the contributor
@DataTypeDefinition {
    name: "Contributer",
    baseType: Element,
    elements: {
        "type": {
            name: "type",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "author | editor | reviewer | endorser"
        },
        "name": {
            name: "name",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Who contributed the content"
        },
        "contact": {
            name: "contact",
            dataType: ContactDetail,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Contact details of the contributor"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type Contributer record {|
    *Element;

    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    ContributerType 'type;
    string name;
    ContactDetail[] contact?;
|};

public enum ContributerType {
    author,
    editor,
    reviewer,
    endorser
}
