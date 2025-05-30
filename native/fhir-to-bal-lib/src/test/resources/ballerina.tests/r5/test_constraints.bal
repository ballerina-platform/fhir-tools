import ballerina/constraint;
import ballerina/test;

@test:Config {}
function testResourceElementConstraints() {
    BodyStructureEu bodyStructure = {
        "resourceType" : "BodyStructure",
        "id" : "example-body-structure-eu",
        "meta" : {
            "profile" : ["http://hl7.eu/fhir/base-r5/StructureDefinition/BodyStructure-eu"]
        },
        "includedStructure" : [
            {
                "structure" : {
                    "coding" : [
                        {
                            "system" : "http://snomed.info/sct",
                            "code" : "8205005",
                            "display" : "Wrist"
                        }
                    ]
                },
                "laterality" : {
                    "coding" : [
                        {
                            "system" : "http://snomed.info/sct",
                            "code" : "7771000",
                            "display" : "Left"
                        }
                    ]
                },
                "qualifier" : [
                    {
                        "coding" : [
                            {
                            "system" : "http://snomed.info/sct",
                            "code" : "351726001",
                            "display" : "Below"
                            }
                        ]
                    }
                ]
            }
        ],
        "patient" : {
            "reference" : "Patient/patient-eu-core-example"
        }
    };

    BodyStructureEu|constraint:Error validate = constraint:validate(bodyStructure, BodyStructureEu);
    if validate is BodyStructureEu {
        test:assertTrue(true, "bodyStructure is valid");
    } else {
        test:assertFail("bodyStructure should not be valid");
    }
}
