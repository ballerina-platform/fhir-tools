// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Details for all kinds of technology-mediated contact points for a person or organization, including telephone, email, etc. 
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + system - [ phone | fax | email | pager | url | sms | other ] ContactPointSystem (Required) (http://hl7.org/fhir/valueset-contact-point-system.html)
# + value - The actual contact point details
# + use - [ home | work | temp | old | mobile ] - purpose of this contact point. ContactPointUse (Required) (http://hl7.org/fhir/valueset-contact-point-use.html)
# + rank - Specify preferred order of use (1 = highest)
# + period - Time period when the contact point was/is in use
@DataTypeDefinition {
    name: "ContactPoint",
    baseType: Element,
    elements: {
        "system" : {
            name: "system",
            dataType: ContactPointSystem,
            min: 0,
            max: 1,
            isArray: false,
            description: "[ phone | fax | email | pager | url | sms | other ] ContactPointSystem (http://hl7.org/fhir/valueset-contact-point-system.html) (Required)"
        },
        "value" : {
            name: "value",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "The actual contact point details"
        },
        "use" : {
            name: "use",
            dataType: ContactPointUse,
            min: 0,
            max: 1,
            isArray: false,
            description: "[ home | work | temp | old | mobile ] - purpose of this contact point. ContactPointUse (Required) (http://hl7.org/fhir/valueset-contact-point-use.html)"
        },
        "rank" : {
            name: "rank",
            dataType: positiveInt,
            min: 0,
            max: 1,
            isArray: false,
            description: "Specify preferred order of use (1 = highest)"
        },
        "period" : {
            name: "period",
            dataType: Period,
            min: 0,
            max: 1,
            isArray: false,
            description: "Time period when the contact point was/is in use"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type ContactPoint record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    ContactPointSystem system?;
    string value?;
    ContactPointUse use?;
    positiveInt rank?;
    Period period?;
|};

public enum ContactPointSystem {
    phone, fax, email, pager, url, sms, other
}

public enum ContactPointUse {
    home, work, temp, old, mobile
}
