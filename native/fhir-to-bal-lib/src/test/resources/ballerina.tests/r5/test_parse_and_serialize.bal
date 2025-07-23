import ballerina/test;
import ballerinax/health.fhir.r5.parser;
import ballerinax/health.fhir.r5;

// Test FHIR ability to parse a given rersource (JSON/XML --> FHIR model)
@test:Config {}
function testParse() returns error? {
    json patientPayload = {
        "resourceType" : "Patient",
        "id" : "patient-eu-core-example",
        "meta" : {
            "profile" : [
            "http://hl7.eu/fhir/base-r5/StructureDefinition/patient-eu-core"
            ]
        },
        "text" : {
            "status" : "generated",
            "div" : "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p class=\"res-header-id\"><b>Generated Narrative: Patient patient-eu-core-example</b></p><a name=\"patient-eu-core-example\"> </a><a name=\"hcpatient-eu-core-example\"> </a><a name=\"patient-eu-core-example-en-US\"> </a><div style=\"display: inline-block; background-color: #d9e0e7; padding: 6px; margin: 4px; border: 1px solid #8da1b4; border-radius: 5px; line-height: 60%\"><p style=\"margin-bottom: 0px\"/><p style=\"margin-bottom: 0px\">Profile: <a href=\"StructureDefinition-patient-eu-core.html\">Patient (EU core)</a></p></div><p style=\"border: 1px #661aff solid; background-color: #e6e6ff; padding: 10px;\">John Doe  Male, DoB: 1980-01-01</p><hr/><table class=\"grid\"><tr><td style=\"background-color: #f3f5da\" title=\"Ways to contact the Patient\">Contact Detail</td><td colspan=\"3\"><ul><li>ph: 555-1234(Home)</li><li>123 Example Street Example City EX 12345 EX </li></ul></td></tr><tr><td style=\"background-color: #f3f5da\" title=\"Patient Links\">Links:</td><td colspan=\"3\"><ul><li>Managing Organization: <a href=\"Organization-organization-eu-core-example.html\">Organization Example Health Organization</a></li></ul></td></tr></table></div>"
        },
        "name" : [
            {
            "family" : "Doe",
            "given" : [
                "John"
            ]
            }
        ],
        "telecom" : [
            {
            "system" : "phone",
            "value" : "555-1234",
            "use" : "home"
            }
        ],
        "gender" : "male",
        "birthDate" : "1980-01-01",
        "address" : [
            {
            "line" : [
                "123 Example Street"
            ],
            "city" : "Example City",
            "state" : "EX",
            "postalCode" : "12345",
            "country" : "EX"
            }
        ],
        "managingOrganization" : {
            "reference" : "Organization/organization-eu-core-example"
        }
    };

    r5:HumanName[] name = [{
        family: "Doe",
        given: ["John"]
    }];

    r5:date birthDate = "1980-01-01";

    r5:Address[] address = [{
        line: ["123 Example Street"],
        city: "Example City",
        state: "EX",
        postalCode: "12345",
        country: "EX"
    }];

    anydata parsedResult = check parser:parse(patientPayload, PatientEuCore);
    PatientEuCore patientModel = check parsedResult.ensureType();
    test:assertEquals(patientModel.name, name, "Patient name is not equal");
    test:assertEquals(patientModel.birthDate, birthDate, "Patient birthDate is not equal");
    test:assertEquals(patientModel.address, address, "Patient address is not equal");
}


// Test FHIR ability to serialize a given resource (FHIR model --> JSON/XML)
@test:Config{}
function testSerialize(){
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

    r5:FHIRResourceEntity fhirEntity = new(patient);
    json|r5:FHIRSerializerError jsonResult = fhirEntity.toJson();
    if jsonResult is json {
        test:assertEquals(true, patient.active);
    } else {
        test:assertFail("Error occurred while serializing the patient model");
    }
}