// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A Signature - XML DigSig, JWS, Graphical image of signature, etc.
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + 'type - Field Description  
# + when - Field Description  
# + who - Field Description  
# + onBehalfOf - Field Description  
# + targetFormat - Field Description  
# + sigFormat - Field Description  
# + data - Field Description
@DataTypeDefinition {
    name: "Signature",
    baseType: Element,
    elements: {
        "type": {
            name: "type",
            dataType: Coding,
            min: 1,
            max: int:MAX_VALUE,
            isArray: true,
            description: "documentation | justification | citation | predecessor | successor | derived-from | depends-on | composed-of",
            valueSet: "https://hl7.org/fhir/valueset-signature-type.html"
        },
        "when": {
            name: "when",
            dataType: instant,
            min: 1,
            max: 1,
            isArray: false,
            description: "When the signature was created"
        },
        "who": {
            name: "who",
            dataType: Reference,
            min: 1,
            max: 1,
            isArray: false,
            description: "When the signature was created"
        },
        "onBehalfOf": {
            name: "onBehalfOf",
            dataType: Reference,
            min: 0,
            max: 1,
            isArray: false,
            description: "When the signature was created"
        },
        "targetFormat": {
            name: "targetFormat",
            dataType: code,
            min: 0,
            max: 1,
            isArray: false,
            description: "When the signature was created"
        },
        "sigFormat": {
            name: "sigFormat",
            dataType: code,
            min: 0,
            max: 1,
            isArray: false,
            description: "When the signature was created"
        },
        "data": {
            name: "data",
            dataType: base64Binary,
            min: 0,
            max: 1,
            isArray: false,
            description: "When the signature was created"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type Signature record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    Coding[] 'type;
    instant when;
    Reference who;
    Reference onBehalfOf?;
    code targetFormat?;
    code sigFormat?;
    base64Binary data?;
|};
