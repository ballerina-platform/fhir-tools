import ballerina/test;
import ballerinax/health.fhir.r5;

@test:Config{}
function testSlicings(){
    BodyStructureEuIncludedStructureBodyLandmarkOrientationDistanceFromLandmark distanceFromLandmark = {
        device: [
            {
                "concept": {
                    "coding": [
                        {
                            "system": "http://snomed.info/sct",
                            "code": "528404",
                            "display": "Measurement device"
                        }
                    ],
                    "text": "Pulse oximeter"
                }
            }
        ]
    };

    BodyStructureEuIncludedStructureBodyLandmarkOrientation bodyLandmarkOrientation = {
        "clockFacePosition": [
            {
                "coding": [
                    {
                        "system": "http://snomed.info/sct",
                        "code": "260330005",
                        "display": "3 o'clock position"
                    }
                ]
            }
        ],
        "distanceFromLandmark": [distanceFromLandmark]
    };

    BodyStructureEuIncludedStructure includedStructure = {
            "structure": {
                "coding": [
                    {
                        "system" : "http://snomed.info/sct",
                        "code" : "8205005",
                        "display" : "Wrist"
                    }
                ]
            },
            "qualifier": [{ coding : [] }],
            "bodyLandmarkOrientation": [bodyLandmarkOrientation]
    };

    BodyStructureEu bodyStructure = {
        "resourceType": "BodyStructure",
        "id": "example-body-structure-eu",
        "meta": {
            "profile": ["http://hl7.eu/fhir/base-r5/StructureDefinition/BodyStructure-eu"]
        },
        "text": {
            "status": "generated",
            "div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p class=\"res-header-id\"><b>Generated Narrative: BodyStructure example-body-structure-eu</b></p><a name=\"example-body-structure-eu\"> </a><a name=\"hcexample-body-structure-eu\"> </a><a name=\"example-body-structure-eu-en-US\"> </a><div style=\"display: inline-block; background-color: #d9e0e7; padding: 6px; margin: 4px; border: 1px solid #8da1b4; border-radius: 5px; line-height: 60%\"><p style=\"margin-bottom: 0px\"/><p style=\"margin-bottom: 0px\">Profile: <a href=\"StructureDefinition-BodyStructure-eu.html\">Body structure (EU base)</a></p></div><p><b>morphology</b>: <span title=\"Codes:{http://snomed.info/sct 339008}\">Blister</span></p><h3>IncludedStructures</h3><table class=\"grid\"><tr><td style=\"display: none\">-</td><td><b>Structure</b></td><td><b>Laterality</b></td><td><b>Qualifier</b></td></tr><tr><td style=\"display: none\">*</td><td><span title=\"Codes:{http://snomed.info/sct 8205005}\">Wrist</span></td><td><span title=\"Codes:{http://snomed.info/sct 7771000}\">Left</span></td><td><span title=\"Codes:{http://snomed.info/sct 351726001}\">Below</span></td></tr></table><p><b>patient</b>: <a href=\"Patient-patient-eu-core-example.html\">John Doe  Male, DoB: 1980-01-01</a></p></div>"
        },
        "morphology": {
            "coding": [
                {
                    "system" : "http://snomed.info/sct",
                    "code" : "339008",
                    "display" : "Blister"
                }
            ]
        },
        "includedStructure": [includedStructure],
        "patient": {
            "reference" : "Patient/patient-eu-core-example"
        }
    };

    test:assertEquals(bodyStructure.includedStructure[0], includedStructure);
}


@test:Config {}
function testFixedValues(){
    CoverageEhicType ehicCoverageType = {"coding": [{}]};

    CoverageEhic euHealthInsuranceCard = {
        "resourceType" : "Coverage",
        "id" : "EhicExampleIt",
        "meta" : {
            "profile" : [
                "http://hl7.eu/fhir/base-r5/StructureDefinition/Coverage-eu-ehic"
            ]
        },
        "contained" : [
            {
            "resourceType" : "Patient",
            "id" : "PatientEhicInline",
            "identifier" : [
                {
                "system" : "http://hl7.it/sid/codiceFiscale",
                "value" : "RSSMRA70A01F205V"
                }
            ],
            "name" : [
                {
                    "family" : "Rossi",
                    "given" : ["Mario"]
                }
            ],
            "birthDate" : "1970-01-01"
            }
        ],
        "identifier" : [
            {
            "system" : "http://example.org/ehic",
            "value" : "80380000900090510553"
            }
        ],
        "status" : "active",
        "kind": "other",
        "type": ehicCoverageType,
        "beneficiary" : {
            "reference" : "#PatientEhicInline"
        },
        "period" : {
            "end" : "2022-01-19"
        },
        "insurer" : {
            "display" : "SSN-MIN SALUTE - 500001"
        }
    };

    CoverageEhicTypeCoding[] coding = euHealthInsuranceCard["type"].coding;
    r5:Coding codingValue = coding[0];

    test:assertEquals(codingValue.system, "http://terminology.hl7.eu/CodeSystem/v3-ActCode");
    test:assertEquals(codingValue.code, "ehic");
    test:assertEquals(codingValue.display, "European Health Insurance Card");
}


@test:Config {}
function testExtensions(){
    PatientEuCore patient = {
        meta:{
            profile: [PROFILE_BASE_PATIENTEUCORE]
        },

        name:[{
            family: "Doe",
            given: ["John"]
        }],

        "extension": [
            {
                "url": 	"http://hl7.org/fhir/StructureDefinition/patient-birthPlace",
                "valueAddress": {
                    "line": [
                        "123 Example Street"
                    ],
                    "city": "Example City",
                    "state": "EX",
                    "postalCode": "12345",
                    "country": "EX"
                }
            },

            {
                "url": "http://hl7.org/fhir/StructureDefinition/patient-sexParameterForClinicalUse",
                "valueString": "male"
            },

            {
                "url": "http://hl7.org/fhir/StructureDefinition/patient-citizenship",
                "valueString": "Austrian"
            },

            {
                "url": "http://hl7.org/fhir/StructureDefinition/patient-nationality",
                "valueString": "Spanish"
            }
        ],

        active: true,
        birthDate: "1980-01-01",
        deceasedBoolean: false
    };

    // cloneWithType: Convert JSON to a user-defined types
    PatientEuCore|error patientClone = patient.cloneWithType(PatientEuCore); 
    if patientClone is error {
        test:assertFail("Error occurred while cloning the patient resource");
    } else {
        test:assertEquals(patientClone.name[0].family, "Doe", "Patient family name is not equal");
        test:assertEquals(patientClone.birthDate, "1980-01-01", "Patient birthDate is not equal");
        test:assertEquals(patientClone.deceasedBoolean, false, "Patient deceasedBoolean is not equal");
    }
}
