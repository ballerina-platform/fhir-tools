// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Human-readable summary of the resource (essential clinical and business information)
# 
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + status - [generated | extensions | additional | empty] NarrativeStatus(http://hl7.org/fhir/valueset-narrative-status.html) (Required)  
# + div - Limited xhtml content
#           * Rule: The narrative SHALL contain only the basic html formatting elements and attributes described in 
#                   chapters 7-11 (except section 4 of chapter 9) and 15 of the HTML 4.0 standard, <a> elements 
#                   (either name or href), images and internally contained style attributes
#           * Rule: The narrative SHALL have some non-whitespace content
@DataTypeDefinition {
    name: "Narrative",
    baseType: Element,
    elements: {
        "status" : {
            name: "status",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "[generated | extensions | additional | empty] NarrativeStatus(http://hl7.org/fhir/valueset-narrative-status.html) (Required)"
        },
        "div" : {
            name: "div",
            dataType: xhtml,
            min: 1,
            max: 1,
            isArray: false,
            description: "Limited xhtml content"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type Narrative record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    StatusCode status;
    xhtml div;
|};

public enum StatusCode {
    generated, extensions, additional, empty
}
