import ballerina/test;
import ballerinax/health.fhir.r4;

@test:Config {}
function testSlicings() {

    USCorePulseOximetryProfileCodeCodingPulseOx pulseOx = {};
    USCorePulseOximetryProfileCodeCodingO2Sat o2Sat = {};

    USCorePulseOximetryProfileCategoryVSCat vsCat = {coding: [{}]};

    USCorePulseOximetryProfile pulseOximetry = {
        id: "pulseOximetry",
        meta: {
            profile: ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-pulse-oximetry"]
        },
        status: "final",
        code: { coding: [ pulseOx, o2Sat ] },
        subject: {
            reference: "Patient/123"
        },
        effectiveDateTime: "2020-01-01T00:00:00Z",
        effectivePeriod: {
            'start: "2020-01-01T00:00:00Z",
            end: "2020-01-01T00:00:00Z"
        }, 
        category: [vsCat]
    };
    test:assertEquals(pulseOximetry.category[0], vsCat);
}

function testFixedValues() {

    USCorePulseOximetryProfileCodeCodingPulseOx pulseOx = {};
    USCorePulseOximetryProfileCodeCodingO2Sat o2Sat = {};

    USCorePulseOximetryProfileCategoryVSCat vsCat = {coding: [{}]};

    USCorePulseOximetryProfile pulseOximetry = {
        id: "pulseOximetry",
        meta: {
            profile: ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-pulse-oximetry"]
        },
        status: "final",
        code: { coding: [ pulseOx, o2Sat ] },
        subject: {
            reference: "Patient/123"
        },
        effectiveDateTime: "2020-01-01T00:00:00Z",
        effectivePeriod: {
            'start: "2020-01-01T00:00:00Z",
            end: "2020-01-01T00:00:00Z"
        }, 
        category: [vsCat]
    };

    r4:CodeableConcept[]? category = pulseOximetry.category;
    
    if category is r4:CodeableConcept[] {
        r4:Coding[]? codings = category[0].coding;
        if codings is r4:Coding[] {
            r4:Coding coding = codings[0];
            test:assertEquals(coding.system, "http://terminology.hl7.org/CodeSystem/observation-category");
            test:assertEquals(coding.code, "vital-signs");
        } else {
            test:assertFail("codings is not populated with fixed values");
        }
    } else {
        test:assertFail("category is not populated with fixed values");
    }
}

@test:Config {}
function testExtensions() {
    json uscorePatient = {
        "resourceType": "Patient",
        "id": "12345",
        "name": [
            {
                "use": "official",
                "family": "Doe",
                "given": [
                    "John"
                ]
            }
        ],
        "gender": "male",
        "identifier": [
            {
                "system": "http://hospital.smarthealthit.org",
                "value": "12345"
            }
        ],
        "extension": [
            {
                "valueCodeableConcept": {
                    "coding": [
                        {
                            "system": "urn:oid:1.2.840.114350.1.13.0.1.7.10.698084.130.657370.19999000",
                            "code": "male",
                            "display": "male"
                        }
                    ]
                },
                "url": "http://open.epic.com/FHIR/StructureDefinition/extension/legal-sex"
            },
            {
                "valueCodeableConcept": {
                    "coding": [
                        {
                            "system": "urn:oid:1.2.840.114350.1.13.0.1.7.10.698084.130.657370.19999000",
                            "code": "male",
                            "display": "male"
                        }
                    ]
                },
                "url": "http://open.epic.com/FHIR/StructureDefinition/extension/sex-for-clinical-use"
            },
            {
                "extension": [
                    {
                        "valueCoding": {
                            "system": "urn:oid:2.16.840.1.113883.6.238",
                            "code": "2106-3",
                            "display": "White"
                        },
                        "url": "ombCategory"
                    },
                    {
                        "valueString": "White",
                        "url": "text"
                    }
                ],
                "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race"
            },
            {
                "extension": [
                    {
                        "valueString": "Unknown",
                        "url": "text"
                    }
                ],
                "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity"
            },
            {
                "valueCodeableConcept": {
                    "coding": [
                        {
                            "system": "http://loinc.org",
                            "code": "LA29518-0",
                            "display": "he/him/his/his/himself"
                        }
                    ]
                },
                "url": "http://open.epic.com/FHIR/StructureDefinition/extension/calculated-pronouns-to-use-for-text"
            }
        ]
    };

    USCorePatientProfile|error patient = uscorePatient.cloneWithType(USCorePatientProfile);
    if patient is error {
        test:assertFail("Error occurred while cloning the patient resource");
    }
}
