// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

import ballerina/time;
import ballerina/jwt;
import wso2healthcare/healthcare.base.message;

# FHIR wire content type formats
public type FHIRWireFormat xml|json;

# Aggregated type of all resource type in this IG
public type AnyBaseResource CodeSystem|ValueSet;

# FHIR Payload formats
public enum FHIRPayloadFormat {
    JSON, XML
}

# FHIR REST Interactions
public enum FHIRInteractionType {
    READ,
    VREAD,
    UPDATE,
    PATCH,
    DELETE,
    HISTORY,
    CREATE,
    SEARCH,
    CAPABILITIES,
    BATCH,
    TRANSACTION
}

# Enum to indicate message flow direction
public enum MessageDirection {
    IN,
    OUT
}

# FHIR search parameter types
public enum FHIRSearchParameterType {
    NUMBER = "Number",
    DATE = "Date",
    STRING = "String",
    TOKEN = "Token",
    REFERENCE = "Reference",
    COMPOSITE = "Composite",
    QUANTITY = "Quantity",
    URI = "URI",
    SPECIAL = "Special"
}

public enum FHIRSearchParameterModifier {
    MODIFIER_ABOVE = "above",
    MODIFIER_BELOW = "below",
    MODIFIER_CODE_TEXT = "code-text",
    MODIFIER_CONTAINS = "contains",
    MODIFIER_EXACT = "exact",
    MODIFIER_IDENTIFIER = "identifier",
    MODIFIER_IN = "in",
    MODIFIER_ITERATE = "iterate",
    MODIFIER_MISSING = "missing",
    MODIFIER_NOT = "not",
    MODIFIER_NOT_IN = "not-in",
    MODIFIER_OF_TYPE = "of-type",
    MODIFIER_TEXT = "text",
    MODIFIER_TEXT_ADVANCED = "text-advanced"
}

# FHIR Interaction levels
public enum FHIRInteractionLevel {
    FHIR_INTERACTION_INSTANCE = "INSTANCE",
    FHIR_INTERACTION_TYPE = "TYPE",
    FHIR_INTERACTION_SYSTEM = "SYSTEM"
}

# For the ordered parameter types of number, date, and quantity, a prefix to the parameter value may be used to 
# control the nature of the matching
public enum Prefix {
    eq,ne,gt,lt,ge,le,sa,eb,ap
}

# Represents a profile, containing summary information
#
# + url - Canonical identifier for this structure definition, represented as a URI (globally unique)
# + resourceType - FHIR resource type
# + modelType - ballerina model type which is used to represent instance of this profile
public type Profile record {|
    string url;
    string resourceType;
    typedesc<anydata> modelType;
|};

# Holds information about MIME details of the request
# + contentType - request content type
# + acceptType - request accept type, JSON will be set as default
type FHIRRequestMimeHeaders record {|
    FHIRPayloadFormat? contentType = ();
    FHIRPayloadFormat acceptType = JSON;
|};

# Search parameter definition representing summary information reqiored for processing from spec
#
# + name - name of the search parameter 
# + 'type -   type of the search parameter
# + base - The resource type this search parameter applies to
# + expression - expression bound to the resource type
public type FHIRSearchParameterDefinition record {
    string name;
    FHIRSearchParameterType 'type;
    string[] base;
    string? expression;
};

# Operation definition representing summary information reqiored for processing from spec
#
# + name - name of the operation
# + instanceLevel - active in instance interaction level
# + typeLevel - active in type interaction level
# + systemLevel - active in system interaction level
public type FHIROperationDefinition record {|
    string name;
    boolean instanceLevel;
    boolean typeLevel;
    boolean systemLevel;
|};

# Record type that holds original incoming values and processed information about the request search parameter
#
# + 'type - Type of search parameter  
# + name - Name of the search parameter (Key of the query parameter)
# + value - Original incoming search parameter value in string format
# + typedValue - Parsed/Decoded search parameter value based on the type of the search parameter
public type RequestSearchParameter record {
    readonly string name;
    readonly string value;
    readonly FHIRSearchParameterType 'type;
    readonly FHIRTypedSearchParameter typedValue;
};

# Base type of decoded/processed search parameter value
# + name - Name of the search parameter
# + modifier - FHIR Search parameter modifier
public type FHIRTypedSearchParameter record {
    readonly string name;
    readonly FHIRSearchParameterModifier|string? modifier;
};

# Number type search parameter information
#
# + value - Value of the received search parameter
# + prefix - prefix is used to control the nature of the matching
public type NumberSearchParameter record {
    *FHIRTypedSearchParameter;
    readonly Prefix prefix = eq;
    readonly int|float value;
};

# String type search parameter information
#
# + value - Value of the received search parameter
public type StringSearchParameter record {
    *FHIRTypedSearchParameter;
    readonly string value;
};

# URI type search parameter information
#
# + uri - URI/URL/URN value
public type URISearchParameter record {
    *FHIRTypedSearchParameter;
    readonly uri uri;
};

# Date type search parameter information
#
# + prefix - Prefix attached with date type search parameter
# + value - Value of the received search parameter
public type DateSearchParameter record {
    *FHIRTypedSearchParameter;
    readonly Prefix prefix;
    //readonly date|dateTime|instant|Period value;
    readonly time:Civil value;
};

# Date type search parameter information
#
# + system - The system property of the Identifier or Coding
# + code - Value of Coding.code or Identifier.value
public type TokenSearchParameter record {
    *FHIRTypedSearchParameter;
    readonly string? system;
    readonly string? code;
};

# Reference type search parameter information
# + resourceType - Referenced resource type
# + id - The logical [id] of a resource using a local reference (i.e. a relative reference)
# + url - Absolute URL - a reference to a resource by its absolute location
public type ReferenceSearchParameter record {
    *FHIRTypedSearchParameter;
    readonly & string? resourceType;
    readonly & string? id;
    readonly & string? url;
};

# Composite type search parameter information record
# + value - Composite search parameter value
public type CompositeSearchParameter record {
    *FHIRTypedSearchParameter;
    readonly string value;
    // TODO : Further imporvement needed : https://github.com/wso2-enterprise/open-healthcare/issues/924
};

# A quantity parameter searches on the Quantity data type
# + prefix - Prefix to the parameter value may be used to control the nature of the matching
# + number - Numerical value (with implicit precision). The number part can be a decimal in exponential format
# + system - System that defines coded unit form
# + code - Coded form of the unit
public type QuantitySearchParameter record {
    *FHIRTypedSearchParameter;
    readonly & Prefix? prefix;
    readonly & int|float number;
    readonly & string? system;
    readonly & string? code;
};

# Special type search parameter information record
# + value - Composite search parameter value
public type SpecialSearchParameter record {
    *FHIRTypedSearchParameter;
    readonly string value;
    // TODO : Further imporvement needed : https://github.com/wso2-enterprise/open-healthcare/issues/924
};

type ModifierMap record {
    readonly & map<string[]> modifierTypeMapping;
};

# Class to wrap FHIR resource
public isolated class FHIRResourceEntity {
    *message:Message;

    private final anydata resourceRecord;

    public isolated function init(anydata resourceRecord) {
        self.resourceRecord = resourceRecord.clone();
    }

    public isolated function unwrap() returns anydata {
        lock {
            return self.resourceRecord.clone();
        }
    }
    public isolated function toXml() returns xml|FHIRSerializerError {
        lock {
            return executeResourceXMLSerializer(self.resourceRecord.clone());
        }
    }

    public isolated function toJson() returns json|FHIRSerializerError {
        lock {
            return executeResourceJsonSerializer(self.resourceRecord.clone());
        }
    }
}

# Class to wrap FHIR resource
public isolated class FHIRContainerResourceEntity {
    *message:Message;

    private final anydata resourceRecord;

    public isolated function init(anydata resourceRecord) {
        self.resourceRecord = resourceRecord.clone();
    }

    public isolated function unwrap() returns anydata {
        lock {
            return self.resourceRecord.clone();
        }
    }

    public isolated function toXml() returns xml|FHIRSerializerError {
        lock {
            return executeBundleXmlSerializer(self.resourceRecord.clone());
        }
    }

    public isolated function toJson() returns json|FHIRSerializerError {
        lock {
            return executeBundleJsonSerializer(self.resourceRecord.clone());
        }
    }
}

# Record Holding information about the HTTP request
#
# + headers - map of incoming HTTP headers
# + payload - HTTP payload
public type HTTPRequest record {
    readonly & map<string[]> headers;
    readonly & (json|xml|string)? payload;
};

# Class representing FHIR request
public isolated class FHIRRequest {
    private final FHIRResourceEntity? resourceEntity;
    
    // Request is bounded to this resource type. Only system level requests result resourceType to be nil
    private final string? resourceType;

    private final readonly & FHIRInteraction interaction;

    private final readonly & map<readonly & RequestSearchParameter[]> searchParameters;
    // TODO FHIR Operations also should go here
    private final FHIRPayloadFormat clientAcceptFormat;

    isolated function init(readonly & FHIRInteraction interaction,
                            string? resourceType,
                            FHIRResourceEntity? resourceEntity,
                            readonly & map<readonly & RequestSearchParameter[]> searchParameters,
                            readonly & FHIRPayloadFormat clientAcceptFormat) {
        self.resourceEntity = resourceEntity;
        self.searchParameters = searchParameters;
        self.clientAcceptFormat = clientAcceptFormat;
        self.resourceType = resourceType;
        self.interaction = interaction;
    }

    public isolated function getResourceEntity() returns FHIRResourceEntity? {
        return self.resourceEntity;
    }

    public isolated function getSearchParameters() returns readonly & map<readonly & RequestSearchParameter[]>{
        return self.searchParameters;
    }

    public isolated function getClientAcceptFormat() returns FHIRPayloadFormat {
        return self.clientAcceptFormat;
    }

    public isolated function getResourceType() returns string? {
        return self.resourceType;
    }

    public isolated function getInteraction() returns readonly & FHIRInteraction {
        return self.interaction;
    }
}

# FHIR Interaction
#
# + interaction - Interaction type
public type FHIRInteraction record {
    FHIRInteractionType interaction;
};

# FHIR Read interaction
#
# + interaction - Interaction type
# + id - target resource id
public type FHIRReadInteraction record {
    *FHIRInteraction;
    
    READ interaction = READ;
    string id;
};

# FHIR serach interaction
#
# + interaction - Interaction type
public type FHIRSearchInteraction record {
    *FHIRInteraction;
    
    SEARCH interaction = SEARCH;
};

# FHIR Create interaction
#
# + interaction - Interaction type
public type FHIRCreateInteraction record {
    *FHIRInteraction;
    
    CREATE interaction = CREATE;
};

# FHIR Update interaction
#
# + interaction - Interaction type
# + id - Target resource id
public type FHIRUpdateInteraction record {
    *FHIRInteraction;

    UPDATE interaction = UPDATE;
    string id;
};

# Class representing FHIR response
public isolated class FHIRResponse {
    private final FHIRResourceEntity resourceEntity;

    isolated function init(FHIRResourceEntity resourceEntity) {
        self.resourceEntity = resourceEntity;
    }

    public isolated function getResourceEntity() returns FHIRResourceEntity {
        return self.resourceEntity;
    }
}

# Class representing FHIR Container response (Bundle)
public isolated class FHIRContainerResponse {
    private final FHIRContainerResourceEntity resourceEntity;

    isolated function init(FHIRContainerResourceEntity resourceEntity) {
        self.resourceEntity = resourceEntity;
    }

    public isolated function getResourceEntity() returns FHIRContainerResourceEntity {
        return self.resourceEntity;
    }
}

# Record to hold information about FHIR related security
#
#
# + securedAPICall - indicate whether the API call is originated from secured API or not
# + fhirUser - FHIR User information 
# + jwt - decoded JWT assertion
public type FHIRSecurity record {
    readonly & boolean securedAPICall;
    readonly & FHIRUser? fhirUser;
    readonly & JWT? jwt;
};

# Record to wrap JWT user information
#
# + scopes - The list of scopes the particular user have.  
# + userID - Unique user identifier.  
# + claims - The user claims given by the identitty provider
public type FHIRUser record {
    readonly & string[] scopes;
    readonly & string userID;
    readonly & map<string> claims;
};

# Record holding Decoded JWT content
#
# + header - JWT header  
# + payload - JWT payload
public type JWT record {
    readonly & jwt:Header header;
    readonly & jwt:Payload payload;
};
