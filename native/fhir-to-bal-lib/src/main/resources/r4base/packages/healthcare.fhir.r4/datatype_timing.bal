// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A timing schedule that specifies an event that may occur multiple times 
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + event - When the event occurs
# + repeat - When the event is to occur  
# + code - Field Description
@DataTypeDefinition {
    name: "Timing",
    baseType: BackboneElement,
    elements: {
        "event": {
            name: "event",
            dataType: dateTime,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "When the event occurs"
        },
        "repeat": {
            name: "repeat",
            dataType: ElementRepeat,
            min: 0,
            max: 1,
            isArray: false,
            description: "When the event is to occur" +
            "+ Rule: if there's a duration, there needs to be duration units" +
            "+ Rule: if there's a period, there needs to be period units" +
            "+ Rule: duration SHALL be a non-negative value" +
            "+ Rule: period SHALL be a non-negative value" +
            "+ Rule: If there's a periodMax, there must be a period" +
            "+ Rule: If there's a durationMax, there must be a duration" +
            "+ Rule: If there's a countMax, there must be a count" +
            "+ Rule: If there's an offset, there must be a when (and not C, CM, CD, CV)" +
            "+ Rule: If there's a timeOfDay, there cannot be a when, or vice versa"
        },
        "code": {
            name: "code",
            dataType: CodeableConcept,
            min: 0,
            max: 1,
            isArray: false,
            description: "BID | TID | QID | AM | PM | QD | QOD | + TimingAbbreviation (Preferred)",
            valueSet: "https://hl7.org/fhir/valueset-timing-abbreviation.html"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}

public type Timing record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    dateTime[] event?;
    ElementRepeat repeat?;
    RepeatCode code?;

|};

public enum RepeatCode {
    BID,
    TID,
    QID,
    AM,
    PM,
    QD,
    QOD,
    Q1H,
    Q2H,
    Q3H,
    Q4H,
    Q6H,
    Q8H,
    BED,
    WK,
    MO
}
