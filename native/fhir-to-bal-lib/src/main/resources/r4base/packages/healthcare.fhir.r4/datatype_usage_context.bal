// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Describes the context of use for a conformance or knowledge resource
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + code - Type of context being specified  
# + valueCodeableConcept - Value that defines the context  
# + valueQuantity - Value that defines the context  
# + valueRange - Value that defines the context 
# + valueReference - Value that defines the context
@DataTypeDefinition {
    name: "UsageContext",
    baseType: Element,
    elements: {
        "code": {
            name: "code",
            dataType: Coding,
            min: 1,
            max: 1,
            isArray: false,
            description: "Type of context being specified",
            valueSet: "http://hl7.org/fhir/ValueSet/usage-context-type"
        },
        "valueCodeableConcept": {
            name: "valueCodeableConcept",
            dataType: CodeableConcept,
            min: 1,
            max: 1,
            isArray: false,
            description: "Value that defines the context"
        },
        "valueQuantity": {
            name: "valueQuantity",
            dataType: Quantity,
            min: 1,
            max: 1,
            isArray: false,
            description: "Value that defines the context"
        },
        "valueRange": {
            name: "valueRange",
            dataType: Range,
            min: 1,
            max: 1,
            isArray: false,
            description: "Value that defines the context"
        },
        "valueReference": {
            name: "valueReference",
            dataType: Reference,
            min: 1,
            max: 1,
            isArray: false,
            description: "Value that defines the context"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    },
    validator: usageContextDataTypeValidationFunction
}
public type UsageContext record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    Coding code;
    CodeableConcept valueCodeableConcept?;
    Quantity valueQuantity?;
    Range valueRange?;
    Reference valueReference?;
|};

public isolated function usageContextDataTypeValidationFunction(anydata data,
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
        CodeableConcept? valueCodeableConceptVal = <CodeableConcept?>mapObj.get("valueCodeableConcept");
        Quantity? valueQuantityVal = <Quantity?>mapObj.get("valueQuantity");
        Range? valueRangeVal = <Range?>mapObj.get("valueRange");
        Reference? valueReferenceVal = <Reference?>mapObj.get("valueReference");

        boolean valueCodeableConceptValCheck = valueCodeableConceptVal is ();
        boolean valueQuantityValCheck = valueQuantityVal is ();
        boolean valueRangeValCheck = valueRangeVal is ();
        boolean valueReferenceValCheck = valueReferenceVal is ();

        boolean expression = (valueCodeableConceptValCheck) && (valueQuantityValCheck) && (valueRangeValCheck) && (!valueReferenceValCheck) 
                        || (valueCodeableConceptValCheck) && (valueQuantityValCheck) && (!valueRangeValCheck) && (valueReferenceValCheck) 
                        || (valueCodeableConceptValCheck) && (!valueQuantityValCheck) && (valueRangeValCheck) && (valueReferenceValCheck)
                        || (!valueCodeableConceptValCheck) && (valueQuantityValCheck) && (valueRangeValCheck) && (valueReferenceValCheck);

        if (expression) {
            return;
        }
        else {
            string diagnosticMsg = "Error occurred due to incorrect definition of timing attribute of UsageContext element " +
                            "according to FHIR Specification";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred due to incorrect data type definition", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg);
        }

    }

}

public type UsageContextValueX CodeableConcept|Quantity|Range|Reference;
