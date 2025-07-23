import ballerina/test;
import ballerinax/health.fhir.r5.international500;
import ballerinax/health.fhir.r5.parser;

@test:Config {}
function testReferredFromInternational() returns error? {
    json internationalBodyStructureInput = {
        "resourceType": "BodyStructure",
        "id": "fetus",
        "text": {
            "status": "generated",
            "div": "<div xmlns='http://www.w3.org/1999/xhtml'></div>"
        },
        "identifier": [
            {
                "system": "http://goodhealth.org/bodystructure/identifiers",
                "value": "12345"
            }
        ],
        "includedStructure": [
            {
                "structure": {
                    "coding": [
                        {
                            "system": "http://snomed.info/sct",
                            "code": "83418008",
                            "display": "Entire fetus (body structure)"
                        }
                    ],
                    "text": "Fetus"
                }
            }
        ],
        "description": "EDD 1/1/2017 confirmation by LMP",
        "patient": {
            "reference": "Patient/example"
        }
    };

    json europeBaseBodyStructureInput = {
        "resourceType": "BodyStructure",
        "id": "example-body-structure-eu",
        "meta": {
            "profile": [
                "http://hl7.eu/fhir/base-r5/StructureDefinition/BodyStructure-eu"
            ]
        },
        "text": {
            "status": "generated",
            "div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p class=\"res-header-id\"><b>Generated Narrative: BodyStructure example-body-structure-eu</b></p><a name=\"example-body-structure-eu\"> </a><a name=\"hcexample-body-structure-eu\"> </a><a name=\"example-body-structure-eu-en-US\"> </a><div style=\"display: inline-block; background-color: #d9e0e7; padding: 6px; margin: 4px; border: 1px solid #8da1b4; border-radius: 5px; line-height: 60%\"><p style=\"margin-bottom: 0px\"/><p style=\"margin-bottom: 0px\">Profile: <a href=\"StructureDefinition-BodyStructure-eu.html\">Body structure (EU base)</a></p></div><p><b>morphology</b>: <span title=\"Codes:{http://snomed.info/sct 339008}\">Blister</span></p><h3>IncludedStructures</h3><table class=\"grid\"><tr><td style=\"display: none\">-</td><td><b>Structure</b></td><td><b>Laterality</b></td><td><b>Qualifier</b></td></tr><tr><td style=\"display: none\">*</td><td><span title=\"Codes:{http://snomed.info/sct 8205005}\">Wrist</span></td><td><span title=\"Codes:{http://snomed.info/sct 7771000}\">Left</span></td><td><span title=\"Codes:{http://snomed.info/sct 351726001}\">Below</span></td></tr></table><p><b>patient</b>: <a href=\"Patient-patient-eu-core-example.html\">John Doe  Male, DoB: 1980-01-01</a></p></div>"
        },
        "morphology": {
            "coding": [
                {
                    "system": "http://snomed.info/sct",
                    "code": "339008",
                    "display": "Blister"
                }
            ]
        },
        "includedStructure": [
            {
                "structure": {
                    "coding": [
                        {
                            "system": "http://snomed.info/sct",
                            "code": "8205005",
                            "display": "Wrist"
                        }
                    ]
                },
                "laterality": {
                    "coding": [
                        {
                            "system": "http://snomed.info/sct",
                            "code": "7771000",
                            "display": "Left"
                        }
                    ]
                },
                "qualifier": [
                    {
                        "coding": [
                            {
                                "system": "http://snomed.info/sct",
                                "code": "351726001",
                                "display": "Below"
                            }
                        ]
                    }
                ]
            }
        ],
        "excludedStructure": [
            {
                "structure": {
                    "coding": [
                        {
                            "system": "http://snomed.info/sct",
                            "code": "83418008",
                            "display": "Entire fetus (body structure)"
                        }
                    ],
                    "text": "Fetus"
                }
            }
        ],
        "patient": {
            "reference": "Patient/patient-eu-core-example"
        }
    };

    international500:BodyStructure international500BodyStructure = check parser:parse(internationalBodyStructureInput, international500:BodyStructure).ensureType();
    international500:BodyStructureIncludedStructure[]? internationalIncludedStructure = international500BodyStructure.includedStructure;

    BodyStructureEu euBodyStructure = check parser:parse(europeBaseBodyStructureInput, BodyStructureEu).ensureType();
    international500:BodyStructureIncludedStructure[]? euExcludedStructure = euBodyStructure.excludedStructure;

    test:assertEquals(internationalIncludedStructure, euExcludedStructure, "Parsed BodyStructure models are not equal");
}

@test:Config {}
function testReferredFromLocal() returns error? {
    json inputEuCorePatient = {
        "resourceType": "Patient",
        "id": "patient-eu-core-example",
        "meta": {
            "profile": [
                "http://hl7.eu/fhir/base-r5/StructureDefinition/patient-eu-core"
            ]
        },
        "text": {
            "status": "generated",
            "div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p class=\"res-header-id\"><b>Generated Narrative: Patient patient-eu-core-example</b></p><a name=\"patient-eu-core-example\"> </a><a name=\"hcpatient-eu-core-example\"> </a><a name=\"patient-eu-core-example-en-US\"> </a><div style=\"display: inline-block; background-color: #d9e0e7; padding: 6px; margin: 4px; border: 1px solid #8da1b4; border-radius: 5px; line-height: 60%\"><p style=\"margin-bottom: 0px\"/><p style=\"margin-bottom: 0px\">Profile: <a href=\"StructureDefinition-patient-eu-core.html\">Patient (EU core)</a></p></div><p style=\"border: 1px #661aff solid; background-color: #e6e6ff; padding: 10px;\">John Doe  Male, DoB: 1980-01-01</p><hr/><table class=\"grid\"><tr><td style=\"background-color: #f3f5da\" title=\"Ways to contact the Patient\">Contact Detail</td><td colspan=\"3\"><ul><li>ph: 555-1234(Home)</li><li>123 Example Street Example City EX 12345 EX </li></ul></td></tr><tr><td style=\"background-color: #f3f5da\" title=\"Patient Links\">Links:</td><td colspan=\"3\"><ul><li>Managing Organization: <a href=\"Organization-organization-eu-core-example.html\">Organization Example Health Organization</a></li></ul></td></tr></table></div>"
        },
        "name": [
            {
                "family": "Doe",
                "given": [
                    "John"
                ]
            }
        ],
        "telecom": [
            {
                "system": "phone",
                "value": "555-1234",
                "use": "home"
            }
        ],
        "gender": "male",
        "birthDate": "1980-01-01",
        "address": [
            {
                "line": [
                    "123 Example Street"
                ],
                "city": "Example City",
                "state": "EX",
                "postalCode": "12345",
                "country": "EX"
            }
        ],
        "managingOrganization": {
            "reference": "Organization/organization-eu-core-example"
        },
        "link": [
            {
                "other": {
                    "reference": "Patient/patient-eu-core-example"
                },
                "type": "refer"
            }
        ],
        "communication": [
            {
                "language": {
                    "coding": [
                        {
                            "system": "urn:ietf:bcp:47",
                            "code": "nl-NL",
                            "display": "Dutch"
                        }
                    ]
                },
                "preferred": true,
                "link": [
                    {
                        "other": {
                            "reference": "Patient/patient-eu-core-example"
                        },
                        "type": "refer"
                    }
                ]
            }
        ]
    };

    PatientEuCore euCorePatientProfile = check parser:parse(inputEuCorePatient, PatientEuCore).ensureType();
    PatientEuLink[]? euCorePatientProfileLink = euCorePatientProfile.link;

    string euCorePatientProfileLinkType = "";
    if euCorePatientProfileLink is PatientEuLink[] {
        euCorePatientProfileLinkType = euCorePatientProfileLink[0].'type;
    }


    string communicationLinkType = "";
    PatientEuCoreCommunication[]? communication = euCorePatientProfile.communication;
    if communication is PatientEuCoreCommunication[] {
        PatientEuLink[]? communicationLink = communication[0].link;

        if communicationLink is PatientEuCoreLink[] {
            communicationLinkType = communicationLink[0].'type;
        }
    }

    test:assertEquals(euCorePatientProfileLinkType, communicationLinkType, "Link type in Patient and Communication is not equal");
}
