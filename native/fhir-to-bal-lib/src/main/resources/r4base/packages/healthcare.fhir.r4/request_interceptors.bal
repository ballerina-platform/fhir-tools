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
import ballerina/uuid;
import ballerina/http;

# Request interceptor to pre-process FHIR read interaction
public isolated service class FHIRReadRequestInterceptor {
    *http:RequestInterceptor;
    private final ResourceAPIConfig apiConfig;
    final FHIRPreprocessor preprocessor;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
        self.preprocessor = new(self.apiConfig);
    }
    
    resource isolated function get fhir/r4/[string resourceType]/[string id] (http:Request request, http:RequestContext ctx) 
                                                                                    returns http:NextService|error? {
        log:printDebug("Execute: fhirReadRequestInterceptor", resourceType = resourceType, id = id);
        return self.preprocessor.processRead(resourceType, id, request, ctx);
    }
}

# Request interceptor to pre-process FHIR vread interaction
public isolated service class FHIRVReadRequestInterceptor {
    *http:RequestInterceptor;
    final ResourceAPIConfig apiConfig;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
    }
    
    resource isolated function get fhir/r4/[string resourceType]/[string id]/_history/[string vid] 
                                    (http:Request request, http:RequestContext ctx) returns http:NextService|FHIRError? {
        log:printDebug("Execute: fhirVReadRequestInterceptor", resourceType = resourceType , id = id, vid = vid);
        // TODO : implement pre-process
        return getNextService(ctx);
    }
}

# Request interceptor to pre-process FHIR update interaction
public isolated service class FHIRUpdateRequestInterceptor {
    *http:RequestInterceptor;
    final ResourceAPIConfig apiConfig;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
    }
    
    resource isolated function put fhir/r4/[string resourceType]/[string id] 
                                    (http:Request request, http:RequestContext ctx) returns http:NextService|FHIRError? {
        log:printDebug("Execute: fhirUpdateRequestInterceptor", resourceType = resourceType , id = id);
        // TODO : implement pre-process
        return getNextService(ctx);
    }
}

# Request interceptor to pre-process FHIR patch interaction
public isolated service class FHIRPatchRequestInterceptor {
    *http:RequestInterceptor;
    final ResourceAPIConfig apiConfig;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
    }
    
    resource isolated function patch fhir/r4/[string resourceType]/[string id] 
                                        (http:Request request, http:RequestContext ctx) returns http:NextService|FHIRError? {
        log:printDebug("Execute: fhirPatchRequestInterceptor", resourceType = resourceType , id = id);
        // TODO : implement pre-process
        return getNextService(ctx);
    }
}

# Request interceptor to pre-process FHIR delete interaction
public isolated service class FHIRDeleteRequestInterceptor {
    *http:RequestInterceptor;
    final ResourceAPIConfig apiConfig;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
    }
    
    resource isolated function delete fhir/r4/[string resourceType]/[string id] 
                                        (http:Request request, http:RequestContext ctx) returns http:NextService|FHIRError? {
        log:printDebug("Execute: fhirDeleteRequestInterceptor", resourceType = resourceType , id = id);
        // TODO : implement pre-process
        return getNextService(ctx);
    }
}

# Request interceptor to pre-process FHIR instance-history-search interaction
public isolated service class FHIRInstanceHistorySearchRequestInterceptor {
    *http:RequestInterceptor;
    final ResourceAPIConfig apiConfig;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
    }
    
    resource isolated function get fhir/r4/[string resourceType]/[string id]/_history 
                                        (http:Request request, http:RequestContext ctx) returns http:NextService|FHIRError? {
        log:printDebug("Execute: fhirInstanceHistorySearchRequestInterceptor", resourceType = resourceType , id = id);
        // TODO : implement pre-process
        return getNextService(ctx);
    }
}

# Request interceptor to pre-process FHIR create interaction
public isolated service class FHIRCreateRequestInterceptor {
    *http:RequestInterceptor;
    final ResourceAPIConfig apiConfig;
    final FHIRPreprocessor preprocessor;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
        self.preprocessor = new(self.apiConfig);
    }
    
    resource isolated function post fhir/r4/[string resourceType] 
                                        (@http:Payload json|xml payload, http:Request httpRequest, 
                                            http:RequestContext ctx) returns http:NextService|FHIRError? {
        log:printDebug("Execute: fhirCreateRequestInterceptor", resourceType = resourceType);
        return self.preprocessor.processCreate(resourceType, payload, httpRequest, ctx);
	}
}

# Request interceptor to pre-process FHIR search interaction
public isolated service class FHIRSearchRequestInterceptor {
    *http:RequestInterceptor;
    final ResourceAPIConfig apiConfig;
    final FHIRPreprocessor preprocessor;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
        self.preprocessor = new(self.apiConfig);
    }
    
    resource isolated function get fhir/r4/[string resourceType] 
                            (http:Request httpRequest, http:RequestContext httpCtx) returns http:NextService|FHIRError? {
        log:printDebug("Execute: fhirSearchRequestInterceptor", 
                                    resourceType = resourceType, searchParameters = httpRequest.getQueryParams());
        return check self.preprocessor.processSearch(resourceType, httpRequest, httpCtx);
    }
}

# Request interceptor to pre-process FHIR history-search interaction
public isolated service class FHIRHistorySearchRequestInterceptor {
    *http:RequestInterceptor;

    final ResourceAPIConfig apiConfig;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
    }
    
    resource isolated function get fhir/r4/[string resourceType]/_history 
                                    (http:Request httpRequest, http:RequestContext ctx) returns http:NextService|error? {
        log:printDebug("Execute: fhirHistorySearchRequestInterceptor", 
                                    resourceType = resourceType, searchParameters = httpRequest.getQueryParams());
        // TODO : implement pre-process
        return getNextService(ctx);
    }
}


# Request error handling interceptor
public isolated service class FHIRRequestErrorInterceptor {
    *http:RequestErrorInterceptor;

    resource isolated function 'default [string... path](http:RequestContext ctx, http:Caller caller, 
                                                        http:Request req, error err) returns http:StatusCodeResponse {
        log:printDebug("Executing FHIR Request Error Interceptor");
        string errorUUID;
        if err is FHIRError {
            FHIRErrorDetail & readonly detail = err.detail();
            if (!detail.internalError) {
                return createHttpErrorResponse(err);
            } else {
                //TODO log the error if it is an internal error
                errorUUID = err.detail().uuid;
                log:printError(string `${errorUUID} : ${err.message()}`, err, err.stackTrace());
            }
        } else {
            // TODO log the error since it is not an FHIR related error
            errorUUID = uuid:createType1AsString();
            log:printError(string `${errorUUID} : ${err.message()}`, err, err.stackTrace());
        }
        OperationOutcome opOutcome = {
            issue: [
                {
                    severity: ERROR,
                    code: PROCESSING,
                    diagnostics: errorUUID
                }
            ]
        };
        http:InternalServerError internalError = {
            body: opOutcome
        };
        return internalError;
   }
}


# GET interceptor to log GET request entries for troubleshooting
public isolated service class FHIRGetRequestInterceptor {
    *http:RequestInterceptor;

    final ResourceAPIConfig apiConfig;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;
    }
    
    resource isolated function 'default [string resourceType]/[string... path] 
                                        (http:Request request, http:RequestContext ctx) returns http:NextService|FHIRError? {
        // TODO implement
        return getNextService(ctx);
    }
}
