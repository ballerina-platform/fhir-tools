// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

json TEST_FHIR_RESOURCE_JSON_PATIENT_01 = {
        "resourceType":"Patient",
        "id":"123344",
        "meta": {
            "profile": [
                "http://hl7.org/fhir/StructureDefinition/Patient"
            ]
        },
        "identifier":[
            {
                "use":"usual",
                "type":{
                    "coding":[
                        {
                            "system":"http://terminology.hl7.org/CodeSystem/v2-0203",
                            "code":"MR"
                        }
                    ]
                },
                "system":"urn:oid:1.2.36.146.595.217.0.1",
                "value":"12345",
                "period":{
                    "start":"2001-05-06"
                },
                "assigner":{
                    "display":"Acme Healthcare"
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
        "telecom":[
            {
                "system":"phone",
                "value":"(03) 5555 6473",
                "use":"work",
                "rank":1
            }
        ],
        "gender":"male",
        "birthDate":"1974-12-25",
        "deceasedBoolean":false,
        "address":[
            {
                "use":"home",
                "type":"both",
                "text":"534 Erewhon St PeasantVille, Rainbow, Vic  3999",
                "line":[
                    "534 Erewhon St"
                ],
                "city":"PleasantVille",
                "district":"Rainbow",
                "state":"Vic",
                "postalCode":"3999",
                "period":{
                    "start":"1974-12-25"
                }
            }
        ],
        "managingOrganization":{
            "reference":"Organization/1"
        }
    };

json TEST_FHIR_RESOURCE_JSON_INVALID_PATIENT_01 = {
        "resourceType":"Patient",
        "id":"123344",
        "meta": {
            "profile": [
                "http://hl7.org/fhir/StructureDefinition/Patient"
            ]
        },
        "identifierr":[
            {
                "use":"usual",
                "type":{
                    "coding":[
                        {
                            "system":"http://terminology.hl7.org/CodeSystem/v2-0203",
                            "code":"MR"
                        }
                    ]
                },
                "system":"urn:oid:1.2.36.146.595.217.0.1",
                "value":"12345",
                "period":{
                    "start":"2001-05-06"
                },
                "assigner":{
                    "display":"Acme Healthcare"
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
        "telecom":[
            {
                "system":"phone",
                "value":"(03) 5555 6473",
                "use":"work",
                "rank":1
            }
        ],
        "gender":"male",
        "birthDate":"1974-12-25",
        "deceasedBoolean":false,
        "address":[
            {
                "use":"home",
                "type":"both",
                "text":"534 Erewhon St PeasantVille, Rainbow, Vic  3999",
                "line":[
                    "534 Erewhon St"
                ],
                "city":"PleasantVille",
                "district":"Rainbow",
                "state":"Vic",
                "postalCode":"3999",
                "period":{
                    "start":"1974-12-25"
                }
            }
        ],
        "managingOrganization":{
            "reference":"Organization/1"
        }
    };
