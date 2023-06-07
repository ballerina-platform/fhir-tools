// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# How the medication is/was taken or should be taken
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + modifierExtension - Field Description  
# + sequence - The order of the dosage instructions  
# + text - Free text dosage instructions e.g. SIG 
# + addtionalInstruction - Supplemental instruction or warnings to the patient - e.g. with meals,may cause drowsiness
# + patientInstruction - Patient or consumer oriented instructions
# + timing - When medication should be administered
# + asNeededBoolean - Take as needed  
# + asNeededCodeableConcept - Take as needed 
# + site - Body site to administer to
# + route - How drug should enter body 
# + method - Technique for administering medication
# + doseAndRate - Amount of medication administered  
# + maxDosePerPeriod - Upper limit on medication per unit of time
# + maxDosePerAdministration - Upper limit on medication per administration 
# + maxDosePerLifetime - Upper limit on medication per lifetime of the patient
@DataTypeDefinition {
    name: "Dosage",
    baseType: BackboneElement,
    elements: {
        "sequence": {
            name: "sequence",
            dataType: integer,
            min: 0,
            max: 1,
            isArray: false,
            description: "The order of the dosage instructions"
        },
        "text": {
            name: "text",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Free text dosage instructions e.g. SIG"
        },
        "additionalInstruction": {
            name: "additionalInstruction",
            dataType: CodeableConcept,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Supplemental instruction or warnings to the patient - e.g. with meals,may cause drowsiness",
            valueSet: "https://hl7.org/fhir/valueset-additional-instruction-codes.html"
        },
        "patientInstruction": {
            name: "patientInstruction",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Patient or consumer oriented instructions"
        },
        "timing": {
            name: "timing",
            dataType: Timing,
            min: 0,
            max: 1,
            isArray: false,
            description: "When medication should be administered"
        },
        "asNeededBoolean": {
            name: "asNeededBoolean",
            dataType: boolean,
            min: 0,
            max: 1,
            isArray: false,
            description: "Take as needed",
            valueSet: "https://hl7.org/fhir/valueset-medication-as-needed-reason.html"
        },
        "asNeededCodeableConcept": {
            name: "asNeededCodeableConcept",
            dataType: CodeableConcept,
            min: 0,
            max: 1,
            isArray: false,
            description: "Take as needed",
            valueSet: "https://hl7.org/fhir/valueset-medication-as-needed-reason.html"
        },
        "site": {
            name: "side",
            dataType: CodeableConcept,
            min: 0,
            max: 1,
            isArray: false,
            description: "Body site to administer to",
            valueSet: "https://hl7.org/fhir/valueset-approach-site-codes.html"
        },
        "route": {
            name: "route",
            dataType: CodeableConcept,
            min: 0,
            max: 1,
            isArray: false,
            description: "How drug should enter body",
            valueSet: "https://hl7.org/fhir/valueset-route-codes.html"
        },
        "method": {
            name: "method",
            dataType: CodeableConcept,
            min: 0,
            max: 1,
            isArray: false,
            description: "Technique for administering medication",
            valueSet: "https://hl7.org/fhir/valueset-administration-method-codes.html"
        },
        "doseAndRate": {
            name: "doseAndRate",
            dataType: ElementDoseAndRate,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Amount of medication administered"
        },
        "maxDosePerPeriod": {
            name: "maxDosePerPeriod",
            dataType: Ratio,
            min: 0,
            max: 1,
            isArray: false,
            description: "Upper limit on medication per unit of time"
        },
        "maxDosePerAdministration": {
            name: "maxDosePerAdministration",
            dataType: SimpleQuantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Upper limit on medication per administration"
        },
        "maxDosePerLifetime": {
            name: "maxDosePerLifetime",
            dataType: SimpleQuantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Upper limit on medication per lifetime of the patient"
        }

    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    },
    validator: dosageDataTypeValidationFunction
}
public type Dosage record {|
    *BackboneElement;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    Extension[] modifierExtension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    integer sequence?;
    string text?;
    CodeableConcept[] addtionalInstruction?;
    string patientInstruction?;
    Timing timing?;
    boolean asNeededBoolean?;
    boolean asNeededCodeableConcept?;
    CodeableConcept site?;
    CodeableConcept route?;
    CodeableConcept method?;
    ElementDoseAndRate[] doseAndRate?;
    Ratio maxDosePerPeriod?;
    SimpleQuantity maxDosePerAdministration?;
    SimpleQuantity maxDosePerLifetime?;
|};

public isolated function dosageDataTypeValidationFunction(anydata data,
                    ElementAnnotationDefinition elementContextDefinition) returns (FHIRValidationError)? {
    DataTypeDefinitionRecord? dataTypeDefinition = (typeof data).@DataTypeDefinition;
    if dataTypeDefinition != () {
        map<anydata> mapObj = {};
        do {
            mapObj = check data.ensureType();
        } on fail error e {
            string diagnosticMsg = "Error occurred while casting data of type: " + (typeof data).toBalString()
                            + " to map representation";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred while casting data to map representation", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg, cause = e);
        }

        boolean? asNeededBooleanVal = <boolean?>mapObj.get("asNeededBoolean");
        CodeableConcept? asNeededCodeableConceptVal = <CodeableConcept?>mapObj.get("asNeededCodeableConcept");

        boolean asNeededBooleanValCheck = asNeededBooleanVal is ();
        boolean asNeededCodeableConceptValCheck = asNeededCodeableConceptVal is ();

        boolean expression = (asNeededBooleanValCheck) && (!asNeededCodeableConceptValCheck) || 
                        (!asNeededBooleanValCheck) && (asNeededCodeableConceptValCheck);

        if (expression) {
            return;
        }
        else {
            string diagnosticMsg = "Error occurred due to incorrect definition of asNeeded attribute in Dosage element " +
                            "according to FHIR Specificatio";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred due to incorrect data type definition", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg);
        }

    }

}
