FHIR Parser module

## Overview

This module provides utility functions required for parsing FHIR resource payloads to corresponding FHIR 
resource models.

### Usage : Parse

**01. Parse to FHIR base resource model**

In this approach user just need to provide only the FHIR resource payload. Based on the resource type, parser will parse 
it to it's base profile model.

```ballerina
json payload = {
    "resourceType": "Patient",
    "id": "1",
    "meta": {
        "profile": [
            "http://hl7.org/fhir/StructureDefinition/Patient"
        ]
    },
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
    "managingOrganization":{
        "reference":"Organization/1"
    }
};
do {
    r4:Patient pat = <r4:Patient> check parser:parse(payload);
} on fail r4:FHIRParseError err {
    log:printError("Error occurred while parsing", err);
}
```

*Note:* `parse` function returns `anydata` type when success, and it needs to be cast to the relevant FHIR Resource type.

**02. Parse to given FHIR profile resource model**

In this approach the parser will attempt to parse the given FHIR resource payload to given resource type

```ballerina
json payload = {
    "resourceType": "Patient",
    "id": "1",
    "meta": {
        "profile": [
            "http://hl7.org/fhir/StructureDefinition/Patient"
        ]
    },
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
    "managingOrganization":{
        "reference":"Organization/1"
    }
};
do {
    r4:Patient pat = <r4:Patient> check parser:parse(payload, r4:Patient);
} on fail r4:FHIRParseError err {
    log:printError("Error occurred while parsing", err);
}
```

*Note:* `parse` function returns `anydata` type when success, and it needs to be cast to the relevant FHIR Resource type.
