// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# The marketing status describes the date when a medicinal product is actually put on the market or 
# the date as of which it is no longer available
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + modifierExtension - Additional content defined by implementations 
# + country - The country in which the marketing authorisation has been granted
# shall be specified It should be specified using the ISO 3166 ‑ 1 alpha-2 code elements  
# + jurisdiction - "Where a Medicines Regulatory Agency has granted a marketing authorisation for 
# which specific provisions within a jurisdiction apply, the jurisdiction can be specified using an
# appropriate controlled terminology The controlled term and the controlled term identifier shall be specified
# + status - This attribute provides information on the status of the marketing of the medicinal product See 
# ISO/TS 20443 for more information and examples  
# + dateRange - The date when the Medicinal Product is placed on the market by the Marketing Authorisation Holder " +
# (or where applicable, the manufacturer/distributor) in a country and/or jurisdiction shall be provided A complete
# date consisting of day, month and year shall be specified using the ISO 8601 date format NOTE “Placed on the market
# refers to the release of the Medicinal Product into the distribution chain
# + restoreDate - The date when the Medicinal Product is placed on the market by the Marketing Authorisation Holder 
# (or where applicable, the manufacturer/distributor) in a country and/or jurisdiction shall be provided A complete 
# date consisting of day, month and year shall be specified using the ISO 8601 date format NOTE “Placed on the market” 
# refers to the release of the Medicinal Product into the distribution chain
@DataTypeDefinition {
    name: "MarketingStatus",
    baseType: BackboneElement,
    elements: {
        "country": {
            name: "country",
            dataType: CodeableConcept,
            min: 0,
            max: 1,
            isArray: false,
            description: "The country in which the marketing authorisation has been granted " +
            "shall be specified It should be specified using the ISO 3166 ‑ 1 alpha-2 code elements"
        },
        "jurisdiction": {
            name: "jurisdiction",
            dataType: CodeableConcept,
            min: 0,
            max: 1,
            isArray: false,
            description: "Where a Medicines Regulatory Agency has granted a marketing authorisation for " +
            "which specific provisions within a jurisdiction apply, the jurisdiction can be specified using " +
            "an appropriate controlled terminology The controlled term and the controlled term identifier shall be specified"
        },
        "status": {
            name: "status",
            dataType: CodeableConcept,
            min: 1,
            max: 1,
            isArray: false,
            description: "This attribute provides information on the status of the marketing of the medicinal product See " +
            "ISO/TS 20443 for more information and examples"
        },
        "dateRange": {
            name: "dateRange",
            dataType: Period,
            min: 0,
            max: 1,
            isArray: false,
            description: "The date when the Medicinal Product is placed on the market by the Marketing Authorisation Holder " +
            "(or where applicable, the manufacturer/distributor) in a country and/or jurisdiction shall be provided A complete " +
            "date consisting of day, month and year shall be specified using the ISO 8601 date format NOTE “Placed on the market” " +
            "refers to the release of the Medicinal Product into the distribution chain"
        },
        "restoreDate": {
            name: "restoreDate",
            dataType: dateTime,
            min: 0,
            max: 1,
            isArray: false,
            description: "The date when the Medicinal Product is placed on the market by the Marketing Authorisation " +
            "Holder (or where applicable, the manufacturer/distributor) in a country and/or jurisdiction shall be provided" +
            " A complete date consisting of day, month and year shall be specified using the ISO 8601 date format NOTE “Placed" +
            " on the market” refers to the release of the Medicinal Product into the distribution chain"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type MarketingStatus record {|
    *BackboneElement;
    //Inherited child element from "BackboneElement" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)
    Extension[] modifierExtension?;
    //Inherited child element from "BackboneElement"

    CodeableConcept country?;
    CodeableConcept jurisdiction?;
    CodeableConcept status;
    Period dateRange?;
    dateTime restoreDate?;
|};
