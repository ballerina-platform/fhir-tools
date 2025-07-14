import ballerina/test;
import ballerinax/health.fhir.r4.parser;
import ballerinax/health.fhir.r4;

@test:Config {}
function testParse() returns error? {
    json patientPayload = {
        "resourceType": "Patient",
        "id": "1",
        "meta": {
            "profile": [
                "http://hl7.org.au/fhir/StructureDefinition/au-patient"
            ]
        },
        "identifier": [
            {
                "use": "usual",
                "type": {
                    "coding": [
                        {
                            "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                            "code": "MR"
                        }
                    ]
                },
                "system": "urn:oid:",
                "value": "57164",
                "_value": {
                    "extension": [
                        {
                            "valueString": "57164",
                            "url": "http://hl7.org/fhir/StructureDefinition/rendered-value"
                        }
                    ]
                }
            }
        ],
        "active":true,
        "name":[
            {
                "use":"official",
                "family":"Chalmers",
                "given":[
                    "Peter",
                    "James"
                ]
            }
        ],
        "gender":"male",
        "birthDate":"1974-12-25",
        "_birthDate": {
             "id": "314159",
             "extension" : [{
                 "url" : "http://example.org/fhir/StructureDefinition/text",
                 "valueString" : "Easter 1970"
             }]
        },
        "managingOrganization":{
            "reference":"Organization/1"
        }
    };

    r4:HumanName[] name = [{
        family: "Chalmers",
        given: ["Peter", "James"],
        use: r4:official
    }];

    anydata parsedResult = check parser:parse(patientPayload, USCorePatientProfile);
    USCorePatientProfile patientModel = check parsedResult.ensureType();
    test:assertEquals(patientModel.name, name, "Patient name is not equal");
}

@test:Config {}
function testSerialize() {
    USCorePatientProfile patient = {
        meta: {
            profile: [PROFILE_BASE_USCOREPATIENTPROFILE]
        },
        active: true,
        name: [{
            family: "Doe",
            given: ["Jhon"],
            use: r4:official,
            prefix: ["Mr"]
        }],
        address: [{
            line: ["652 S. Lantern Dr."],
            city: "New York",
            country: "United States",
            postalCode: "10022",
            'type: r4:physical,
            use: r4:home
        }],
        identifier: [{
            system: "http://acme.org/mrns",
            value: "12345"
        }], 
        gender: "unknown"
    };
    r4:FHIRResourceEntity fhirEntity = new(patient);
    json|r4:FHIRSerializerError jsonResult = fhirEntity.toJson();
    if jsonResult is json {
        test:assertEquals(true, patient.active);
    } else {
        test:assertFail("Error occurred while serializing the patient model");
    }
}
