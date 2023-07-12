// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

import ballerina/http;

# Function to validate against API Config and parse FHIR resource payload
#
# + payload - FHIR resource payload  
# + apiConfig - APIConfig of the FHIR resource API
# + return - Record representation of the FHIR resource if success.
isolated function validateAndParse(json|xml payload, ResourceAPIConfig apiConfig) 
                                                            returns anydata|FHIRValidationError|FHIRParseError? {
    readonly & Profile profile = check validateAndExtractProfile(payload, apiConfig);
    return check parseFHIRResource(profile, payload);
}

# Function to parse FHIR Payload into FHIR Resource model
#   Note : When using inside FHIR templates, use healthcare.fhir.r4utils.parser module instead of this
# 
# + payload - FHIR payload  
# + targetFHIRModelType - (Optional) target model type to parse. Derived from payload if not given
# + return - returns FHIR model (Need to cast to relevant type by the caller). FHIRParseError if error ocurred
public isolated function parse (json|xml payload, typedesc<anydata>? targetFHIRModelType = ()) returns anydata|FHIRParseError {
    string|FHIRValidationError resourceType = extractResourceType(payload);
    if resourceType is FHIRValidationError {
        return <FHIRParseError> createParserErrorFrom(resourceType);
    }
    Profile resourceProfile;
    if targetFHIRModelType is () {
        (Profile & readonly)? profile = fhirRegistry.findBaseProfile(resourceType);
        if profile is (Profile & readonly) {
            resourceProfile = profile.clone();
        } else {
            string msg = "Failed to find FHIR profile for the resource type : " + resourceType;
            return <FHIRParseError> createFHIRError(msg, ERROR, INVALID_STRUCTURE, errorType = PARSE_ERROR);
        }
    } else {
        ResourceDefinitionRecord|FHIRTypeError resourceDefinition = getResourceDefinition(targetFHIRModelType);
        if resourceDefinition is ResourceDefinitionRecord {
            string? profile = resourceDefinition.profile;
            if profile != () {
                map<Profile & readonly> & readonly perResourceProfiles = fhirRegistry.getResourceProfiles(resourceType);
                if perResourceProfiles.hasKey(profile) {
                    resourceProfile = perResourceProfiles.get(profile);
                } else {
                    string msg = "Failed to find FHIR profile for the resource type : " + resourceType;
                    return <FHIRParseError> createFHIRError(msg, ERROR, INVALID_STRUCTURE, errorType = PARSE_ERROR);
                }
            } else {
                string msg = "Failed to find FHIR profile in the definition of the resource type : " + resourceType;
                return <FHIRParseError> createFHIRError(msg, ERROR, INVALID_STRUCTURE, errorType = PARSE_ERROR);
            }
        } else {
            return createParserErrorFrom(resourceDefinition);
        }
    }
    return parseFHIRResource(resourceProfile, payload);
}

isolated function validateAndExtractProfile(json|xml payload, ResourceAPIConfig apiConfig) 
                                                                    returns (readonly & Profile)|FHIRValidationError {
    string resourceType = check extractResourceType(payload);
    if !fhirRegistry.isSupportedResource(resourceType) {
        string diag = "Payload contains unknown resource type : " + resourceType;
        return <FHIRValidationError> createFHIRError("Unknown FHIR resource type", ERROR, INVALID, diag,
                                                errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
    }

    if resourceType != apiConfig.resourceType {
        string msg = "Mismatching resource type of the FHIR resource with the resource API";
        string diagMsg = "Payload resource type :\"" + resourceType + 
                                        "\" but expected resource type :\"" + apiConfig.resourceType + "\"";
        return <FHIRValidationError> createFHIRError(msg, ERROR, INVALID, diagMsg,
                                                errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
    }

    string[]? profiles = extractProfiles(payload);
    if profiles != () && profiles.length() > 0 {
        map<Profile & readonly> & readonly resourceProfiles = fhirRegistry.getResourceProfiles(resourceType);
        // validate profiles
        foreach string profile in profiles {
            // check whether the profile is a valid profile
            if !resourceProfiles.hasKey(profile) {
                string diag = "Unknown profile : " + profile;
                return <FHIRValidationError> createFHIRError("Invalid FHIR profile", ERROR, INVALID, 
                                diagnostic = diag, errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
            }

            // check whether the profile is supported according to API config
            if apiConfig.profiles.indexOf(profile) is () {
                string diag = "FHIR server does not this FHIR profile : " + profile;
                return <FHIRValidationError> createFHIRError("Unsupported FHIR profile", ERROR, INVALID,
                                diagnostic = diag, errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
            }
        }
        
        if profiles.length() == 1 {
            return fhirRegistry.getResourceProfiles(resourceType).get(profiles[0]);
        } else {
            // If there are multiple profiles, we select the matching default profile if configured
            // otherwise, default profile will be the base profile.
            string? defaultProfile = apiConfig.defaultProfile;
            if defaultProfile != () {
                return resourceProfiles.get(defaultProfile);
            }
        }
    }
    // get base IG profile (we reach here if profile is not mentioned in the request or if the request contains multiple
    // resources with no default profile in API config)
    (Profile & readonly)? profile = fhirRegistry.findBaseProfile(resourceType);
    if profile != () {
        return profile;
    }
    string diag = "Matching profile not found for the resource type : " + resourceType;
    return <FHIRValidationError> createFHIRError("Profile not found", ERROR, PROCESSING,
                                diagnostic = diag, errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
    
}

isolated function parseFHIRResource(Profile profile, json|xml payload) returns anydata|FHIRParseError {
    if payload is json {
        anydata|error cloneWithType = payload.cloneWithType(profile.modelType);
        if cloneWithType is error {
            return <FHIRParseError>createFHIRError("Failed to parse request body as JSON resource", ERROR, PROCESSING, 
                                                    cloneWithType.message(), cause = cloneWithType, errorType = PARSE_ERROR, 
                                                    httpStatusCode = http:STATUS_BAD_REQUEST);
        } else {
            return cloneWithType;
        }
    } else {
        // TODO: parse xml payload [https://github.com/wso2-enterprise/open-healthcare/issues/887]
        return <FHIRParseError>createFHIRError("XML format of FHIR resources not supported yet", ERROR, 
                                                PROCESSING_NOT_SUPPORTED, errorType = PARSE_ERROR,
                                                httpStatusCode = http:STATUS_NOT_IMPLEMENTED);
    }
}

isolated function extractProfiles(json|xml payload) returns string[]? {
    if payload is json {
        json|error profiles = payload?.meta?.profile;
        if profiles is json[] {
            string[] result = [];
            foreach json profile in profiles {
                if profile is string {
                    result.push(profile);
                }
            }
            return result;
        }
    } else {
        // TODO handle XML payload [https://github.com/wso2-enterprise/open-healthcare/issues/887]
    }
    return ();
}

isolated function extractResourceType(json|xml payload) returns string|FHIRValidationError {
    if payload is json {
        map<json> jsonPayload = <map<json>> payload;
        if jsonPayload.hasKey("resourceType") {
            json jResourceType = jsonPayload.get("resourceType");
            if jResourceType is string {
                return jResourceType;
            }
        }

    } else {
        // TODO handle XML payload [https://github.com/wso2-enterprise/open-healthcare/issues/887]
        return <FHIRValidationError>createFHIRError("XML format of FHIR resources not supported yet", ERROR, 
                                                PROCESSING_NOT_SUPPORTED, errorType = VALIDATION_ERROR, 
                                                httpStatusCode = http:STATUS_NOT_IMPLEMENTED);
    }
    string message = "Failed to parse request body as JSON resource";
    string diagnostic = "Invalid JSON content detected, missing required element: \"resourceType\"";
    return <FHIRValidationError>createFHIRError(message, ERROR, PROCESSING, diagnostic = diagnostic, 
                                errorType = VALIDATION_ERROR);
}
