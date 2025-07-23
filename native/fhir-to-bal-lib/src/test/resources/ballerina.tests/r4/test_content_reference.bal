import ballerina/test;
import ballerinax/health.fhir.r4.international401;
import ballerinax/health.fhir.r4.parser;

@test:Config {}
function testReferredFromInternational() returns error? {
    json internationalObservationInput = {
        "resourceType": "Observation",
        "id": "satO2",
        "meta": {
            "profile": ["http://hl7.org/fhir/StructureDefinition/vitalsigns"]
        },
        "text": {
            "status": "generated",
            "div": "<div></div>"
        },
        "identifier": [
            {
                "system": "http://goodcare.org/observation/id",
                "value": "o1223435-10"
            }
        ],
        "partOf": [
            {
                "reference": "Procedure/ob"
            }
        ],
        "status": "final",
        "code": {
            "coding": [
                {
                    "system": "http://loinc.org",
                    "code": "2708-6",
                    "display": "Oxygen saturation in Arterial blood"
                },
                {
                    "system": "http://loinc.org",
                    "code": "59408-5",
                    "display": "Oxygen saturation in Arterial blood by Pulse oximetry"
                },
                {
                    "system": "urn:iso:std:iso:11073:10101",
                    "code": "150456",
                    "display": "MDC_PULS_OXIM_SAT_O2"
                }
            ]
        },
        "subject": {
            "reference": "Patient/example"
        },
        "effectiveDateTime": "2014-12-05T09:30:10+01:00",
        "valueQuantity": {
            "value": 95,
            "unit": "%",
            "system": "http://unitsofmeasure.org",
            "code": "%"
        },
        "device": {
            "reference": "DeviceMetric/example"
        },
        "referenceRange": [
            {
                "low": {
                    "value": 90,
                    "unit": "%",
                    "system": "http://unitsofmeasure.org",
                    "code": "%"
                },
                "high": {
                    "value": 99,
                    "unit": "%",
                    "system": "http://unitsofmeasure.org",
                    "code": "%"
                }
            }
        ]
    };

    json pulseOximetryProfileInput = {
        "resourceType": "Observation",
        "id": "satO2-fiO2",
        "meta": {
            "profile": [
                "http://hl7.org/fhir/us/core/StructureDefinition/us-core-pulse-oximetry|8.0.0"
            ]
        },
        "text": {
            "status": "generated",
            "div": "<div></div>"
        },
        "identifier": [
            {
                "system": "http://example.org/FHIR/observation/identifiers",
                "value": "o1223435-10"
            }
        ],
        "status": "final",
        "category": [
            {
                "coding": [
                    {
                        "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                        "code": "vital-signs",
                        "display": "Vital Signs"
                    }
                ],
                "text": "Vital Signs"
            }
        ],
        "code": {
            "coding": [
                {
                    "system": "http://loinc.org",
                    "code": "2708-6",
                    "display": "Oxygen saturation in Arterial blood"
                },
                {
                    "system": "http://loinc.org",
                    "code": "59408-5",
                    "display": "Oxygen saturation in Arterial blood by Pulse oximetry"
                },
                {
                    "system": "urn:iso:std:iso:11073:10101",
                    "code": "150456",
                    "display": "MDC_PULS_OXIM_SAT_O2"
                }
            ]
        },
        "subject": {
            "reference": "Patient/example"
        },
        "effectiveDateTime": "2014-12-05T09:30:10+01:00",
        "valueQuantity": {
            "value": 95,
            "unit": "%",
            "system": "http://unitsofmeasure.org",
            "code": "%"
        },
        "interpretation": [
            {
                "coding": [
                    {
                        "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
                        "code": "N",
                        "display": "Normal"
                    }
                ],
                "text": "Normal"
            }
        ],
        "device": {
            "display": "Acme Pulse Oximeter 2000"
        },
        "referenceRange": [
            {
                "low": {
                    "value": 90,
                    "unit": "%",
                    "system": "http://unitsofmeasure.org",
                    "code": "%"
                },
                "high": {
                    "value": 99,
                    "unit": "%",
                    "system": "http://unitsofmeasure.org",
                    "code": "%"
                }
            }
        ],
        "component": [
            {
                "code": {
                    "coding": [
                        {
                            "system": "http://loinc.org",
                            "code": "3151-8",
                            "display": "Inhaled oxygen flow rate"
                        }
                    ],
                    "text": "Inhaled oxygen flow rate"
                },
                "valueQuantity": {
                    "value": 6,
                    "unit": "liters/min",
                    "system": "http://unitsofmeasure.org",
                    "code": "L/min"
                },
                "referenceRange": [
                    {
                        "low": {
                            "value": 90,
                            "unit": "%",
                            "system": "http://unitsofmeasure.org",
                            "code": "%"
                        },
                        "high": {
                            "value": 99,
                            "unit": "%",
                            "system": "http://unitsofmeasure.org",
                            "code": "%"
                        }
                    }
                ]
            }
        ]
    };

    international401:Observation international401Observation = check parser:parse(internationalObservationInput, international401:Observation).ensureType();
    international401:ObservationReferenceRange[]? observationReferenceRange = international401Observation.referenceRange;

    USCorePulseOximetryProfile usCorePulseOximetryProfile = check parser:parse(pulseOximetryProfileInput, USCorePulseOximetryProfile).ensureType();
    USCorePulseOximetryProfileComponent[]? components = usCorePulseOximetryProfile.component;

    if components is USCorePulseOximetryProfileComponent[] {
        USCorePulseOximetryProfileComponent component = components[0];
        test:assertEquals(component.referenceRange, observationReferenceRange, "Reference ranges in component are not equal");
    }
}

@test:Config {}
function testReferredFromLocal() returns error? {
    json inputProvenance = {
        "resourceType": "Provenance",
        "id": "example",
        "text": {
            "status": "generated",
            "div": "<div xmlns='http://www.w3.org/1999/xhtml'></div>"
        },
        "target": [
            {
                "reference": "Procedure/example/_history/1"
            }
        ],
        "occurredPeriod": {
            "start": "2015-06-27",
            "end": "2015-06-28"
        },
        "recorded": "2015-06-27T08:39:24+10:00",
        "policy": [
            "http://acme.com/fhir/Consent/25"
        ],
        "location": {
            "reference": "Location/1"
        },
        "reason": [
            {
                "coding": [
                    {
                        "system": "http://snomed.info/sct",
                        "code": "3457005",
                        "display": "Referral"
                    }
                ]
            }
        ],
        "agent": [
            {
                "type": {
                    "coding": [
                        {
                            "system": "http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                            "code": "AUT"
                        }
                    ]
                },
                "who": {
                    "reference": "Practitioner/xcda-author"
                }
            },
            {
                "id": "a1",
                "type": {
                    "coding": [
                        {
                            "system": "http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                            "code": "DEV"
                        }
                    ]
                },
                "who": {
                    "reference": "Device/software"
                }
            }
        ],
        "entity": [
            {
                "role": "source",
                "what": {
                    "reference": "DocumentReference/example",
                    "display": "CDA Document in XDS repository"
                },
                "agent": [
                    {
                        "type": {
                            "coding": [
                                {
                                    "system": "http://hl7.org/fhir/us/core/CodeSystem/us-core-provenance-participant-type",
                                    "code": "transmitter"
                                }
                            ]
                        },
                        "who": {
                            "reference": "Organization/1",
                            "display": "Acme Healthcare"
                        }
                    }   
                ]
            }
        ]
    };

    USCoreProvenance usCoreProvenanceProfile = check parser:parse(inputProvenance, USCoreProvenance).ensureType();
    USCoreProvenanceEntity[]? entity = usCoreProvenanceProfile.entity;
    if entity is USCoreProvenanceEntity[] {
        USCoreProvenanceAgentProvenanceTransmitter[]? provenanceTransmitter = entity[0].agent;

        if provenanceTransmitter is USCoreProvenanceAgentProvenanceTransmitter[] {
            test:assertEquals(provenanceTransmitter[0].who.reference, "Organization/1", "Provenance transmitter reference is not as expected");
        }
    }
}
