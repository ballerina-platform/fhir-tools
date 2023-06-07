// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

import ballerina/log;

public const string PROFILE_BASE_BUNDLE = "http://hl7.org/fhir/StructureDefinition/Bundle";
public const RESOURCE_NAME_BUNDLE = "Bundle";


@ContainerDefinition {
    name: "Bundle",
    baseType: Resource,
    profile: PROFILE_BASE_BUNDLE,
    elements: {
        "resourceType" : {
            name: "resourceType",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Type of the resource [Bundle]"
        },
        "identifier" : {
            name: "identifier",
            dataType: Identifier,
            min: 0,
            max: 1,
            isArray: false,
            description: "Persistent identifier for the bundle"
        },
        "type" : {
            name: "type",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "document | message | transaction | transaction-response | batch | batch-response | history | searchset | collection",
            valueSet: "http://hl7.org/fhir/ValueSet/bundle-type"
        },
        "timestamp" : {
            name: "timestamp",
            dataType: instant,
            min: 0,
            max: 1,
            isArray: false,
            description: "When the bundle was assembled"
        },
        "total" : {
            name: "total",
            dataType: unsignedInt,
            min: 0,
            max: 1,
            isArray: false,
            description: "If search, the total number of matches"
        },
        "link" : {
            name: "link",
            dataType: BundleLink,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Links related to this Bundle"
        },
        "entry" : {
            name: "entry",
            dataType: BundleEntry,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Entry in the bundle - will have a resource or information"
        },
        "signature" : {
            name: "signature",
            dataType: Signature,
            min: 0,
            max: 1,
            isArray: false,
            description: "Digital Signature"
        }
    },
    serializers: {
        'xml: fhirBundleXmlSerializer, 
        'json: fhirBundleJsonSerializer
    },
    validator: validateFHIRResource
}
public type Bundle record {|
    *Resource;
    //Inherited child element from "Resource" (Redefining to maintain order when serialize) (START)
    RESOURCE_NAME_BUNDLE resourceType = RESOURCE_NAME_BUNDLE;
    string id?;
    BaseBundleMeta meta = {
        profile : [PROFILE_BASE_BUNDLE]
    };
    uri implicitRules?;
    code language?;
    //Inherited child element from "Resource" (Redefining to maintain order when serialize) (END)

    Identifier identifier?;
    BundleType 'type;
    instant timestamp?;
    unsignedInt total?;
    BundleLink[] link?;
    BundleEntry[] entry?;
    Signature signature?;
|};

@ContainerDefinition {
    name: "Bundle",
    baseType: Resource,
    profile: PROFILE_BASE_BUNDLE,
    elements: {
        "resourceType" : {
            name: "resourceType",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Type of the resource [Bundle]"
        },
        "identifier" : {
            name: "identifier",
            dataType: Identifier,
            min: 0,
            max: 1,
            isArray: false,
            description: "Persistent identifier for the bundle"
        },
        "type" : {
            name: "type",
            dataType: code,
            min: 1,
            max: 1,
            isArray: false,
            description: "document | message | transaction | transaction-response | batch | batch-response | history | searchset | collection",
            valueSet: "http://hl7.org/fhir/ValueSet/bundle-type"
        },
        "timestamp" : {
            name: "timestamp",
            dataType: instant,
            min: 0,
            max: 1,
            isArray: false,
            description: "When the bundle was assembled"
        },
        "total" : {
            name: "total",
            dataType: unsignedInt,
            min: 0,
            max: 1,
            isArray: false,
            description: "If search, the total number of matches"
        },
        "link" : {
            name: "link",
            dataType: BundleLink,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Links related to this Bundle"
        },
        "entry" : {
            name: "entry",
            dataType: BundleEntry,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Entry in the bundle - will have a resource or information"
        },
        "signature" : {
            name: "signature",
            dataType: Signature,
            min: 0,
            max: 1,
            isArray: false,
            description: "Digital Signature"
        }
    },
    serializers: {
        'xml: fhirBundleXmlSerializer, 
        'json: fhirBundleJsonSerializer
    },
    validator: validateFHIRResource
}
public type BundleWireModel record {|
    *Resource;
    //Inherited child element from "Resource" (Redefining to maintain order when serialize) (START)
    RESOURCE_NAME_BUNDLE resourceType = RESOURCE_NAME_BUNDLE;
    string id?;
    BaseBundleMeta meta = {
        profile : [PROFILE_BASE_BUNDLE]
    };
    uri implicitRules?;
    code language?;
    //Inherited child element from "Resource" (Redefining to maintain order when serialize) (END)

    Identifier identifier?;
    BundleType 'type;
    instant timestamp?;
    unsignedInt total?;
    BundleLink[] link?;
    BundleEntryModel[] entry?;
    Signature signature?;
|};


@DataTypeDefinition {
    name: "BaseBundleMeta",
    baseType: Meta,
    elements: {},
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type BaseBundleMeta record {|
    *Meta;
    
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    id versionId?;
    instant lastUpdated?;
    uri 'source?;
    canonical[] profile = [PROFILE_BASE_BUNDLE];
    Coding[] security?;
    Coding[] tag?;
|};

public enum BundleType {
    BUNDLE_TYPE_DOCUMENT="document",
    BUNDLE_TYPE_MESSAGE="message",
    BUNDLE_TYPE_TRANSACTION="transaction",
    BUNDLE_TYPE_TRANSACTION_RESPONSE="transaction-response",
    BUNDLE_TYPE_BATCH="batch",
    BUNDLE_TYPE_BATCH_RESPONSE="batch-response",
    BUNDLE_TYPE_HISTORY="history",
    BUNDLE_TYPE_SEARCHSET="searchset",
    BUNDLE_TYPE_COLLECTION="collection"
}

@DataTypeDefinition {
    name: "",
    baseType: (),
    elements: {
        "relation" : {
            name: "relation",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "See http://www.iana.org/assignments/link-relations/link-relations.xhtml#link-relations-1"
        },
        "url" : {
            name: "url",
            dataType: uri,
            min: 1,
            max: 1,
            isArray: false,
            description: "Reference details for the link"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type BundleLink record {|
    string relation;
    uri url;
|};

@DataTypeDefinition {
    name: "BundleEntry",
    baseType: (),
    elements: {
        "link" : {
            name: "link",
            dataType: BundleLink,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Links related to this entry"
        },
        "fullUrl" : {
            name: "fullUrl",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "URI for resource (Absolute URL server address or URI for UUID/OID)"
        },
        "resource" : {
            name: "resource",
            dataType: FHIRResourceEntity,
            min: 0,
            max: 1,
            isArray: false,
            description: "A resource in the bundle"
        },
        "search" : {
            name: "search",
            dataType: BundleEntrySearch,
            min: 0,
            max: 1,
            isArray: false,
            description: "Search related information"
        },
        "request" : {
            name: "request",
            dataType: BundleEntryRequest,
            min: 0,
            max: 1,
            isArray: false,
            description: "Additional execution information (transaction/batch/history)"
        },
        "response" : {
            name: "response",
            dataType: BundleEntryResponse,
            min: 0,
            max: 1,
            isArray: false,
            description: "Results of execution (transaction/batch/history)"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type BundleEntry record {|

    BundleLink[] link?;
    uri fullUrl?;
    anydata|FHIRWireFormat 'resource?;
    BundleEntrySearch search?;
    BundleEntryRequest request?;
    BundleEntryResponse response?;
|};

@DataTypeDefinition {
    name: "BundleEntryModel",
    baseType: (),
    elements: {
        "link" : {
            name: "link",
            dataType: BundleLink,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Links related to this entry"
        },
        "fullUrl" : {
            name: "fullUrl",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "URI for resource (Absolute URL server address or URI for UUID/OID)"
        },
        "resource" : {
            name: "resource",
            dataType: FHIRResourceEntity,
            min: 0,
            max: 1,
            isArray: false,
            description: "A resource in the bundle"
        },
        "search" : {
            name: "search",
            dataType: BundleEntrySearch,
            min: 0,
            max: 1,
            isArray: false,
            description: "Search related information"
        },
        "request" : {
            name: "request",
            dataType: BundleEntryRequest,
            min: 0,
            max: 1,
            isArray: false,
            description: "Additional execution information (transaction/batch/history)"
        },
        "response" : {
            name: "response",
            dataType: BundleEntryResponse,
            min: 0,
            max: 1,
            isArray: false,
            description: "Results of execution (transaction/batch/history)"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type BundleEntryModel record {|

    BundleLink[] link?;
    uri fullUrl?;
    AnyBaseResource|FHIRWireFormat 'resource?;
    BundleEntrySearch search?;
    BundleEntryRequest request?;
    BundleEntryResponseModel response?;
|};

@DataTypeDefinition {
    name: "BundleEntrySearch",
    baseType: (),
    elements: {
        "mode" : {
            name: "mode",
            dataType: SearchEntryMode,
            min: 0,
            max: 1,
            isArray: false,
            description: "match | include | outcome - why this is in the result set"
        },
        "score" : {
            name: "score",
            dataType: decimal,
            min: 0,
            max: 1,
            isArray: false,
            description: "Search ranking (between 0 and 1)"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type BundleEntrySearch record {|
    SearchEntryMode mode?;
    decimal score?;
|};


public enum SearchEntryMode {
    MATCH="match",
    INCLUDE="include",
    OUTCOME="outcome"
}


@DataTypeDefinition {
    name: "BundleEntryRequest",
    baseType: (),
    elements: {
        "method" : {
            name: "method",
            dataType: HTTPVerb,
            min: 1,
            max: 1,
            isArray: false,
            description: "GET | HEAD | POST | PUT | DELETE | PATCH"
        },
        "url" : {
            name: "url",
            dataType: uri,
            min: 1,
            max: 1,
            isArray: false,
            description: "URL for HTTP equivalent of this entry"
        },
        "ifNoneMatch" : {
            name: "ifNoneMatch",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "For managing cache currency"
        },
        "ifModifiedSince" : {
            name: "ifModifiedSince",
            dataType: instant,
            min: 0,
            max: 1,
            isArray: false,
            description: "For managing cache currency"
        },
        "ifMatch" : {
            name: "ifMatch",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "For managing update contention"
        },
        "ifNoneExist" : {
            name: "ifNoneExist",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "For conditional creates"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type BundleEntryRequest record {|
    HTTPVerb method;
    uri url;
    string ifNoneMatch?;
    instant ifModifiedSince?;
    string ifMatch?;
    string ifNoneExist?;
|};


public enum HTTPVerb {
    GET, HEAD, POST, PUT, DELETE, PATCH
}

@DataTypeDefinition {
    name: "BundleEntryResponse",
    baseType: (),
    elements: {
        "status" : {
            name: "status",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Status response code (text optional)"
        },
        "location" : {
            name: "location",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "The location (if the operation returns a location)"
        },
        "etag" : {
            name: "etag",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "The Etag for the resource (if relevant)"
        },
        "lastModified" : {
            name: "lastModified",
            dataType: instant,
            min: 0,
            max: 1,
            isArray: false,
            description: "Server's date time modified"
        },
        "outcome" : {
            name: "outcome",
            dataType: FHIRResourceEntity,
            min: 0,
            max: 1,
            isArray: false,
            description: "OperationOutcome with hints and warnings (for batch/transaction)"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type BundleEntryResponse record {|
    string status;
    uri location?;
    string etag?;
    instant lastModified?;
    anydata|FHIRWireFormat outcome?;
|};

public type BundleEntryResponseModel record {|
    string status;
    uri location?;
    string etag?;
    instant lastModified?;
    AnyBaseResource|FHIRWireFormat outcome?;
|};


isolated function executeBundleJsonSerializer(anydata bundle) returns (json|FHIRSerializerError) {
    log:printDebug("Execute: Bundle JSON serializer executer");
    ContainerDefinitionRecord? resourceDef = (typeof bundle).@ContainerDefinition;
    if resourceDef != () {
        if bundle is Bundle {
            ContainerSerializerFunction serializerFunc = resourceDef.serializers.'json;
            FHIRWireFormat wireFormat = check serializerFunc(bundle);
            if wireFormat is json {
                return wireFormat;
            } else {
                string diagnosticMsg = "XML Serializer did not return a XML result. It returned: " +
                                                                        (typeof wireFormat).toBalString();
                return <FHIRSerializerError>createInternalFHIRError("Resource Serialization failed",FATAL, PROCESSING, 
                                                                        diagnostic = diagnosticMsg);
            }
        } else {
            string diagnosticMsg = "Unknown/unsupported data type : " + (typeof bundle).toBalString() + ", expected : r4:Bundle";
            return <FHIRSerializerError>createInternalFHIRError("Unknown/unsupported data type", FATAL, PROCESSING, 
                                                                        diagnostic = diagnosticMsg);
        }
    } else {
        string diagnosticMsg = "Resource definition not found of the record : " + (typeof bundle).toBalString();
        return <FHIRSerializerError>createInternalFHIRError("Resource definition not found",FATAL, PROCESSING, 
                                                                        diagnostic = diagnosticMsg);
    }
}

isolated function executeBundleXmlSerializer(anydata bundle) returns xml|FHIRSerializerError {
    ContainerDefinitionRecord? resourceDef = (typeof bundle).@ContainerDefinition;
    if resourceDef != () {
        if bundle is Bundle {
            ContainerSerializerFunction serializerFunc = resourceDef.serializers.'xml;
            FHIRWireFormat xmlWireFormat = check serializerFunc(bundle);
            if xmlWireFormat is xml {
                return xmlWireFormat;
            } else {
                string diagnosticMsg = "XML Serializer did not return a XML result. It returned: " +
                                                                                    (typeof xmlWireFormat).toBalString();
                return <FHIRSerializerError>createInternalFHIRError("Resource Serialization failed",FATAL, PROCESSING, 
                                                                        diagnostic = diagnosticMsg);
            }
        } else {
            string diagnosticMsg = "Expected a Bundle (map<value:Cloneable>) but found : " + (typeof bundle).toBalString();
            return <FHIRSerializerError>createInternalFHIRError("Unexpected data type for the bundle",FATAL, PROCESSING, 
                                                                    diagnostic = diagnosticMsg);
        }
    } else {
        string diagnosticMsg = "Resource definition not found of the record : " + (typeof bundle).toBalString();
        return <FHIRSerializerError>createInternalFHIRError("Resource definition not found",FATAL, PROCESSING, 
                                                                    diagnostic = diagnosticMsg);
    }
}

public isolated function fhirBundleXmlSerializer(Bundle data) returns (FHIRWireFormat|FHIRSerializerError)? {
    BundleWireModel bundle = check bundleToWireModel(data, JSON);
    // TODO Implement
    return bundle.toJson();
    
}
public isolated function fhirBundleJsonSerializer(Bundle data) returns (FHIRWireFormat|FHIRSerializerError)? {
    
    BundleWireModel bundle = check bundleToWireModel(data, JSON);
    return bundle.toJson();
    
}


// Private Fucntions

// Function to transform Bundle to BundleModel
isolated function bundleToWireModel(Bundle bundle, FHIRPayloadFormat format) returns BundleWireModel|FHIRSerializerError { 

    BundleWireModel result = {
        'type: bundle.'type
    };

    string? id = bundle.id;
    if id is string {
        result.id = id;
    }

    BaseBundleMeta? meta = bundle.meta;
    if meta is BaseBundleMeta {
        result.meta = meta;
    }

    uri? implicitRules = bundle.implicitRules;
    if implicitRules is uri {
        result.implicitRules = implicitRules;
    }

    code? language = bundle.language;
    if language is code {
        result.language = language;
    }

    Identifier? identifier = bundle.identifier;
    if identifier is Identifier {
        result.identifier = identifier;
    }

    instant? timestamp = bundle.timestamp;
    if timestamp is instant {
        result.timestamp = timestamp;
    }

    unsignedInt? total = bundle.total;
    if total is unsignedInt {
        result.total = total;
    }

    BundleLink[]? link = bundle.link;
    if link is BundleLink[] {
        result.link = link;
    }
    
    // Transform entries to model entry
    BundleEntry[]? entries = bundle.entry;
    if entries != () {
        BundleEntryModel[] modelEntries = []; 
        foreach BundleEntry entry in entries {
            BundleEntryModel bEntryModel = {};

            BundleLink[]? entryLink = entry.link;
            if entryLink is BundleLink[] {
                bEntryModel.link = entryLink;
            }

            uri? fullUrl = entry.fullUrl;
            if fullUrl is uri {
                bEntryModel.fullUrl = fullUrl;
            }

            if entry.hasKey("resource") {
                anydata|FHIRWireFormat entryResource = <anydata|FHIRWireFormat>entry.get("resource");
                bEntryModel.'resource = check serializeEntryResource(entryResource, format);
            }

            BundleEntrySearch? search = entry.search;
            if search is BundleEntrySearch {
                bEntryModel.search = search;
            }

            BundleEntryRequest? request = entry.request;
            if request is BundleEntryRequest {
                bEntryModel.request = request;
            }

            //transform entry response to response model
            BundleEntryResponse? response = entry.response;
            if response is BundleEntryResponse {
                BundleEntryResponseModel responseModel = {
                    status: response.status
                };
                
                uri? location = response.location;
                if location is uri {
                    responseModel.location = location;
                }

                string? etag = response.etag;
                if etag is string {
                    responseModel.etag = etag;
                }

                instant? lastModified = response.lastModified;
                if lastModified is instant {
                    responseModel.lastModified = lastModified;
                }

                if response.hasKey("outcome") {
                    anydata|FHIRWireFormat entryResource = <anydata|FHIRWireFormat>entry.get("outcome");
                    responseModel.outcome = check serializeEntryResource(entryResource, format);
                }
                bEntryModel.response = responseModel;
            }
            modelEntries.push(bEntryModel);
        }
        if modelEntries.length() > 0 {
            result.entry = modelEntries;
        }
    }

    Signature? signature = bundle.signature;
    if signature is Signature {
        result.signature = signature;
    }

    return result;
}


isolated function serializeEntryResource (anydata|FHIRWireFormat entryResource, FHIRPayloadFormat format) returns FHIRWireFormat|FHIRSerializerError {
    if entryResource is FHIRWireFormat {
        FHIRWireFormat wireResource = <FHIRWireFormat>entryResource;
        if (wireResource is json && format == JSON) || (wireResource is xml && format == XML) {
            return wireResource;
        } else {
            string diagnosticMsg = "Expected type : " + format + ", but found " + (typeof wireResource).toBalString();
            return <FHIRSerializerError>createInternalFHIRError("Mismatching resource entry format",FATAL, PROCESSING, 
                                                                        diagnostic = diagnosticMsg);
        }
    } else {
        FHIRResourceEntity resourceEntity = new(entryResource);
        if format is JSON {
            return check resourceEntity.toJson();
        } else {
            return check resourceEntity.toXml();
        }
    }
}
