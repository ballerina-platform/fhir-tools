// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Defines an expected trigger for a module
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + 'type - named-event | periodic | data-changed | data-added | data-modified | data-removed | data-accessed | data-access-ended  
# + name - Name or URI that identifies the event  
# + timingTiming - Timing of the event
# + timingReference - Timing of the event  
# + timingDate - Timing of the event 
# + timingDateTime - Timing of the event  
# + data - Triggering data of the event (multiple = 'and')  
# + condition - Whether the event triggers (boolean expression)
@DataTypeDefinition {
    name: "TriggerDefinition",
    baseType: Element,
    elements: {
        "type": {
            name: "type",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "named-event | periodic | data-changed | data-added | data-modified | data-removed | data-accessed | data-access-ended"
        },
        "name": {
            name: "name",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Name or URI that identifies the event"
        },
        "timingTiming": {
            name: "timingTiming",
            dataType: Timing,
            min: 0,
            max: 1,
            isArray: false,
            description: "Timing of the event"
        },
        "timingReference": {
            name: "timingReference",
            dataType: Reference,
            min: 0,
            max: 1,
            isArray: false,
            description: "Timing of the event"
        },
        "timingDate": {
            name: "timingDate",
            dataType: date,
            min: 0,
            max: 1,
            isArray: false,
            description: "Timing of the event"
        },
        "timingDateTime": {
            name: "timingDateTime",
            dataType: dateTime,
            min: 0,
            max: 1,
            isArray: false,
            description: "Timing of the event"
        },
        "data": {
            name: "data",
            dataType: DataRequirement,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Triggering data of the event (multiple = 'and')"
        },
        "condition": {
            name: "condition",
            dataType: Expression,
            min: 0,
            max: 1,
            isArray: false,
            description: "Whether the event triggers (boolean expression)"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    },
    validator: triggerDefinitionDataTypeValidationFunction
}
public type TriggerDefinition record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    code 'type;
    string name?;
    Timing timingTiming?;
    Reference timingReference?;
    date timingDate?;
    dateTime timingDateTime?;
    DataRequirement[] data?;
    Expression condition?;

|};

public isolated function triggerDefinitionDataTypeValidationFunction(anydata data,
                    ElementAnnotationDefinition elementContextDefinition) returns (FHIRValidationError)? {
    DataTypeDefinitionRecord? dataTypeDefinition = (typeof data).@DataTypeDefinition;
    if dataTypeDefinition != () {
        map<anydata> mapObj = {};
        do {
            mapObj = check data.ensureType();
        } on fail error e {
            string diagnosticMsg = "Error occurred while casting data of type: " +
                            (typeof data).toBalString() + " to map representation";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred while casting data to map representation", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg, cause = e);

        }
        Timing? timingTimingVal = <Timing?>mapObj.get("timingTiming");
        Reference? timingReferenceVal = <Reference?>mapObj.get("timingReference");
        date? timingDateVal = <date?>mapObj.get("timingDate");
        dateTime? timingDateTimeVal = <dateTime?>mapObj.get("timingDateTime");

        boolean timingTimingValCheck = timingTimingVal is ();
        boolean timingReferenceValCheck = timingReferenceVal is ();
        boolean timingDateValCheck = timingDateVal is ();
        boolean timingDateTimeValCheck = timingDateTimeVal is ();

        boolean expression = (timingTimingValCheck) && (timingReferenceValCheck) && (timingDateValCheck) && (!timingDateTimeValCheck) 
                        || (timingTimingValCheck) && (timingReferenceValCheck) && (!timingDateValCheck) && (timingDateTimeValCheck) 
                        || (timingTimingValCheck) && (!timingReferenceValCheck) && (timingDateValCheck) && (timingDateTimeValCheck)
                        || (!timingTimingValCheck) && (timingReferenceValCheck) && (timingDateValCheck) && (timingDateTimeValCheck);

        if (expression) {
            return;
        }
        else {
            string diagnosticMsg = "Error occurred due to incorrect definition of timing attribute " +
                            "of TriggerDefinition element according to FHIR Specificatio";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred due to incorrect data type definition", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg);
        }

    }

}
