// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Description
#
# + id - Field Description  
# + extension - Field Description  
# + use - [home | work | temp | old | billing] - purpose of this address AddressUse (Required) (http://hl7.org/fhir/valueset-address-use.html) 
# + 'type - [postal | physical | both] AddressType (Required) (http://hl7.org/fhir/valueset-address-type.html)
# + text - Text representation of the address
# + line - Street name, number, direction & P.O. Box etc.
# + city - Name of city, town etc.
# + district - District name (aka county)
# + state - Sub-unit of country (abbreviations ok)
# + postalCode - Postal code for area
# + country - Country (e.g. can be ISO 3166 2 or 3 letter code)
# + period - Time period when address was/is in use
@DataTypeDefinition {
    name: "Address",
    baseType: Element,
    elements: {
        "use" : {
            name: "use",
            dataType: code,
            min: 0,
            max: 1,
            isArray: false,
            description: "[home | work | temp | old | billing] - purpose of this address AddressUse (Required)",
            path: "Patient.use"
        },
        "type" : {
            name: "type",
            dataType: code,
            min: 0,
            max: 1,
            isArray: false,
            description: "[postal | physical | both] AddressType (Required)",
            path: "Patient.type"
        },
        "text" : {
            name: "text",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Text representation of the address",
            path: "Patient.text"
        },
        "line" : {
            name: "line",
            dataType: string,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Street name, number, direction & P.O. Box etc.",
            path: "Patient.line"
        },
        "city" : {
            name: "city",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Name of city, town etc.",
            path: "Patient.city"
        },
        "district" : {
            name: "district",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "District name (aka county)",
            path: "Patient.district"
        },
        "state" : {
            name: "state",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Sub-unit of country (abbreviations ok)",
            path: "Patient.state"
        },
        "postalCode" : {
            name: "postalCode",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Postal code for area",
            path: "Patient.postalCode"
        },
        "country" : {
            name: "country",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Country (e.g. can be ISO 3166 2 or 3 letter code)",
            path: "Patient.country"
        },
        "period" : {
            name: "period",
            dataType: Period,
            min: 0,
            max: 1,
            isArray: false,
            description: "Time period when address was/is in use",
            path: "Patient.period"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type Address record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    AddressUse use?;
    AddressType 'type?;
    string text?;
    string[] line?;
    string city?;
    string district?;
    string state?;
    string postalCode?;
    string country?;
    Period period?;

|};

public enum AddressUse {
    home, work, temp, old, billing
}

public enum AddressType {
    postal, physical, both
}
