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
import ballerina/log;

# Processes FHIR interactions by triggering the registered source connect implementations.
#
# + sourceInteraction - a record holding implementation function signatures of each FHIR interaction.  
# + ctx - HTTP context of the incomig request.
# + return - FHIR Error if any error occured while processing. Nil otherwise
public isolated function processFHIRSourceConnections(FHIRSourceConnectInteraction sourceInteraction, http:RequestContext ctx) returns FHIRError? {

    FHIRContext fhirContext = check getFHIRContext(ctx);
    map<RequestSearchParameter[]> paramsArr = fhirContext.getRequestSearchParameters();
    if fhirContext.getInteraction().interaction.equalsIgnoreCaseAscii(SEARCH) {
        FHIRContainerResourceEntity entity;
        Bundle bundle = {
            'type: BUNDLE_TYPE_SEARCHSET
        };
        BundleEntry[] entry = [];

        if paramsArr.hasKey(FHIR_SEARCH_PARAM_PROFILE) {
            foreach RequestSearchParameter param in paramsArr.get(FHIR_SEARCH_PARAM_PROFILE) {
                log:printDebug(string `Profile URL: ${param.value}`);
                ctx.set("_OH_activeProfile", param.value);
                searchFunction searchFunct = sourceInteraction.search;
                BundleEntry[]|FHIRError? search = searchFunct(paramsArr, ctx);

                if search is FHIRError {
                    return search;
                } else if search is () {
                    string diag = "Unable to find FHIR resource in source systems.";
                    return createInternalFHIRError("FHIR Resource not found", WARNING, PROCESSING_NOT_FOUND, diagnostic = diag);
                } else {
                    foreach BundleEntry item in search {
                        entry.push(item);
                    }
                }
                bundle.entry = entry;
            }
        } else {
            searchFunction searchFunct = sourceInteraction.search;
            BundleEntry[]|FHIRError? search = searchFunct(paramsArr, ctx);

            if search is FHIRError {
                return search;
            } else if search is () {
                string diag = "Unable to find FHIR resource in source systems.";
                return createInternalFHIRError("FHIR Resource not found", WARNING, PROCESSING_NOT_FOUND, diagnostic = diag);
            } else {
                foreach BundleEntry item in search {
                    entry.push(item);
                }
            }
            bundle.entry = entry;
        }

        entity = new (bundle);
        check setResponseResourceEntity(entity, ctx);
        return;
    } else if fhirContext.getInteraction().interaction.equalsIgnoreCaseAscii(READ) {
        string resourceId = (<FHIRReadInteraction>fhirContext.getInteraction()).id;
        readFunction readFunct = sourceInteraction.read;
        FHIRResourceEntity|FHIRError read = readFunct(resourceId, ctx);

        if read is FHIRError {
            return read;
        } else {
            check setResponseResourceEntity(read, ctx);
            return;
        }
    } else if fhirContext.getInteraction().interaction.equalsIgnoreCaseAscii(CREATE) {
        //respond loacation header. https://github.com/wso2-enterprise/open-healthcare/issues/1091
        FHIRRequest? fhirRequest = fhirContext.getFHIRRequest();
        FHIRResourceEntity? entity = ();
        if fhirRequest is FHIRRequest {
            entity = fhirRequest.getResourceEntity();
        }
        FHIRResourceEntity samplePayload = (new ("testString"));
        createFunction createFunct = sourceInteraction.create;

        string|FHIRError create = createFunct(entity ?: samplePayload, ctx);

        if create is FHIRError {
            return create;
        } else {
            //set location header here.
        }
        return;
    } else if fhirContext.getInteraction().interaction.equalsIgnoreCaseAscii(UPDATE) {
        //respond 200 if updated, 201 with location header if created
        string resourceId = (<FHIRReadInteraction>fhirContext.getInteraction()).id;
        FHIRRequest? fhirRequest = fhirContext.getFHIRRequest();

    } else if fhirContext.getInteraction().interaction.equalsIgnoreCaseAscii(DELETE) {
        //if system keeps version history, delete operation should not remove version history
        FHIRRequest? fhirRequest = fhirContext.getFHIRRequest();

    }
    string diag = "Unable to process FHIR interaction.";
    return createInternalFHIRError("Error in processing request", FATAL, PROCESSING_NOT_SUPPORTED, diagnostic = diag);

}

# Function type to be used for read interaction implementation.
public type readFunction isolated function (string id, http:RequestContext ctx) returns FHIRResourceEntity|FHIRError;

# Function type to be used for search interaction implementation.
public type searchFunction isolated function (map<RequestSearchParameter[]> params, http:RequestContext ctx) returns BundleEntry[]|FHIRError?;

# Function type to be used for create interaction implementation.
public type createFunction isolated function (FHIRResourceEntity entity, http:RequestContext ctx) returns string|FHIRError;

# FHIR interaction implementation function signatures will be communicated via this record.
#
# + read - Read interaction implementation function signature
# + search - Search interaction implementation function signature  
# + create - Create interaction implementation function signature
public type FHIRSourceConnectInteraction record {|
    readonly readFunction read;
    readonly searchFunction search;
    readonly createFunction create;
|};
