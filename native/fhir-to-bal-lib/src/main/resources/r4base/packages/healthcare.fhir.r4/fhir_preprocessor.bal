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
import ballerina/http;
import ballerina/jwt;
import ballerina/regex;

# FHIR Pre-processor implementation
public isolated class FHIRPreprocessor {

    final ResourceAPIConfig apiConfig;
    // All the active search parameters
    private final readonly & map<SearchParamConfig> searchParamConfigMap;

    public isolated function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;

        map<SearchParamConfig> searchParamConfigs = {};
        // process common seach parameters
        foreach CommonSearchParameterDefinition item in COMMON_SEARCH_PARAMETERS {
            SearchParamConfig searchParamConfig = {
                name: item.name,
                active: true
            };
            searchParamConfigs[item.name] = searchParamConfig;
        }

        // process resource specific seach parameters
        foreach SearchParamConfig item in self.apiConfig.searchParameters {
            searchParamConfigs[item.name] = item;
        }
        self.searchParamConfigMap = searchParamConfigs.cloneReadOnly();
    }

    public isolated function processRead (string fhirResourceType, string id, http:Request httpRequest, http:RequestContext httpCtx)
                                                                                    returns http:NextService|FHIRError? {
        log:printDebug("Pre-processing FHIR interaction : read");
        // Validate main HTTP headers
        FHIRRequestMimeHeaders clientHeaders = check validateClientRequestHeaders(httpRequest);

        // Create interaction
        readonly & FHIRReadInteraction readInteraction = {id: id};

        // Create FHIR request
        FHIRRequest fhirRequest = new(readInteraction, fhirResourceType, (), {}, clientHeaders.acceptType);

        // Populate JWT information in FHIR context
        readonly & FHIRSecurity fhirSecurity = check getFHIRSecurity(httpRequest);

        HTTPRequest & readonly request = createHTTPRequestRecord(httpRequest, ());

        // Create FHIR context
        FHIRContext fCtx = new(fhirRequest, request, fhirSecurity);

        // Set FHIR context inside HTTP context
        setFHIRContext(fCtx, httpCtx);

        return getNextService(httpCtx);
    }

    public isolated function processSearch (string fhirResourceType, http:Request httpRequest, http:RequestContext httpCtx)
                                                                                    returns http:NextService|FHIRError? {
        log:printDebug("Pre-processing FHIR interaction : search");
        
        // Validate main HTTP headers
        FHIRRequestMimeHeaders clientHeaders = check validateClientRequestHeaders(httpRequest);
        
        // extract search parameters from request
        map<RequestSearchParameter[]> requestSearchParameters = 
                                                    check self.processSearchParameters(fhirResourceType, httpRequest);

        // Create interaction
        readonly & FHIRSearchInteraction searchInteraction = {};

        FHIRRequest fhirRequest = new(searchInteraction,
                                    fhirResourceType,
                                    (),
                                    requestSearchParameters.cloneReadOnly(),
                                    clientHeaders.acceptType);

        // Populate JWT information in FHIR context
        readonly & FHIRSecurity fhirSecurity = check getFHIRSecurity(httpRequest);

        HTTPRequest & readonly request = createHTTPRequestRecord(httpRequest, ());

        // Create FHIR context
        FHIRContext fCtx = new(fhirRequest, request, fhirSecurity);

        // Set FHIR context inside HTTP context
        setFHIRContext(fCtx, httpCtx);

        return getNextService(httpCtx);
    }

    public isolated function processCreate (string resourceType, json|xml payload, http:Request httpRequest,
                                                http:RequestContext httpCtx) returns http:NextService|FHIRError? {
        log:printDebug("Pre-processing FHIR interaction : Create");
        // Validate main HTTP headers
        FHIRRequestMimeHeaders clientHeaders = check validateClientRequestHeaders(httpRequest);

        if self.apiConfig.resourceType != resourceType {
            string diagMsg = "Request path level resource type : \"" + resourceType + 
                                "\" does not match API config resource type: \"" + self.apiConfig.resourceType + "\"";
            return  createInternalFHIRError("API resource type and API config does not match", ERROR, PROCESSING, diagnostic = diagMsg);
        }

        // Validate and parse payload to FHIR resource model and create resource entity
        anydata parsedResource = check validateAndParse(payload, self.apiConfig);
        FHIRResourceEntity resourceEntity = new(parsedResource);

        // Create interaction
        readonly & FHIRCreateInteraction createInteraction = {};

        // Create FHIR request
        FHIRRequest fhirRequest = new(createInteraction, resourceType, resourceEntity, {}, clientHeaders.acceptType);

        // Populate JWT information in FHIR context
        readonly & FHIRSecurity fhirSecurity = check getFHIRSecurity(httpRequest);

        HTTPRequest & readonly request = createHTTPRequestRecord(httpRequest, payload);

        // Create FHIR context
        FHIRContext fhirCtx = new(fhirRequest, request, fhirSecurity);

        // Set FHIR context inside HTTP context
        setFHIRContext(fhirCtx, httpCtx);

        return getNextService(httpCtx);
    }


    isolated function processSearchParameters(string fhirResourceType, http:Request request) 
                                                                returns map<RequestSearchParameter[]>|FHIRError {
        map<RequestSearchParameter[]> processedSearchParams = {};
        SearchParamCollection searchParamDefinitions = fhirRegistry.getResourceSearchParameters(fhirResourceType);

        map<string[]> requestQueryParams = request.getQueryParams();
        foreach string originalParamName in requestQueryParams.keys() {

            // Decode search parameter key and seperate name and modifier
            // Refer: http://hl7.org/fhir/search.html#modifiers
            RequestQueryParameter queryParam = 
                            check decodeSearchParameterKey(originalParamName, requestQueryParams.get(originalParamName));
            RequestSearchParameter[] processResult;
            if (searchParamDefinitions.hasKey(queryParam.name)) {
                // Processing search parameters bound to resource
                log:printDebug(string `Processing resource bound search parameter: ${queryParam.name}`);

                // check whether it is active in the resource API config
                if self.searchParamConfigMap.hasKey(queryParam.name) && self.searchParamConfigMap.get(queryParam.name).active {

                    SearchParamConfig & readonly searchParamConfig = self.searchParamConfigMap.get(queryParam.name);
                    FHIRSearchParameterDefinition parameterDef = searchParamDefinitions.get(queryParam.name);

                    // pre process search parameters
                    processResult = check processResourceBoundSearchParameter(parameterDef, fhirResourceType, 
                                                                                searchParamConfig, queryParam); 

                } else {
                    string diagnose = "Unsupported search parameter \"" + queryParam.name + "\" for resource type \""+ 
                                        fhirResourceType +"\". Supported search parameters are: " +
                                        extractActiveSearchParameterNames(self.searchParamConfigMap).toBalString();
                    return createFHIRError("Unsupported search parameter : " + queryParam.name, ERROR, PROCESSING, 
                                                    diagnostic = diagnose, httpStatusCode = http:STATUS_BAD_REQUEST);
                }
            } else if COMMON_SEARCH_PARAMETERS.hasKey(queryParam.name) {
                // processing common search parameter
                log:printDebug(string `Processing common search parameter: ${queryParam.name}`);
                CommonSearchParameterDefinition parameterDef = COMMON_SEARCH_PARAMETERS.get(queryParam.name);

                processResult = check processCommonSearchParameter(parameterDef, fhirResourceType, queryParam, 
                                                                    self.apiConfig, self.searchParamConfigMap);

            } else if CONTROL_SEARCH_PARAMETERS.hasKey(queryParam.name) {
                // processing control search parameter
                log:printDebug(string `Processing control search parameter: ${queryParam.name}`);
                CommonSearchParameterDefinition parameterDef = CONTROL_SEARCH_PARAMETERS.get(queryParam.name);

                processResult = check processCommonSearchParameter(parameterDef, fhirResourceType, queryParam, 
                                                                        self.apiConfig, self.searchParamConfigMap);

            } else {
                string diagnose = "Unknown search parameter \"" + queryParam.name + "\" for resource type \""+ 
                                        fhirResourceType +"\". Valid/Supported search parameters for this search are: " +
                                        self.searchParamConfigMap.keys().toString();
                return createFHIRError("Unknown search parameter : " + queryParam.name, ERROR, PROCESSING,
                                            diagnostic = diagnose, httpStatusCode = http:STATUS_BAD_REQUEST);
            }

            // add process result to pre-processed search parameter map
            if processResult.length() > 0 {
                if processedSearchParams.hasKey(queryParam.name) {
                    RequestSearchParameter[] paramArray = processedSearchParams.get(queryParam.name);
                    foreach RequestSearchParameter item in processResult {
                        paramArray.push(item);
                    }
                } else {
                    processedSearchParams[queryParam.name] = processResult;
                }
            }
        }

        // process rest of the common search parameters and populate default values
        foreach CommonSearchParameterDefinition sParam in COMMON_SEARCH_PARAMETERS {
            if !processedSearchParams.hasKey(sParam.name) {
                RequestSearchParameter? defaultParam = check getCommonSearchParamDefault(sParam, self.apiConfig);
                if defaultParam != () {
                    processedSearchParams[sParam.name] = [defaultParam];
                }
            }
        }
        // process rest of the search control parameters and populate default values
        foreach CommonSearchParameterDefinition sParam in CONTROL_SEARCH_PARAMETERS {
            if !processedSearchParams.hasKey(sParam.name) {
                RequestSearchParameter? defaultParam = check getCommonSearchParamDefault(sParam, self.apiConfig);
                if defaultParam != () {
                    processedSearchParams[sParam.name] = [defaultParam];
                }
            }
        }

        return processedSearchParams;
    }
}

isolated function setFHIRContext(FHIRContext ctx, http:RequestContext httpCtx) {
    httpCtx.set(FHIR_CONTEXT_PROP_NAME, ctx);
}

isolated function getNextService(http:RequestContext httpCtx) returns http:NextService?|FHIRError {
    http:NextService|error? next = httpCtx.next();
    if next is error {
        return createInternalFHIRError("Error occurred while retrieving next service", ERROR, PROCESSING, cause = next);
    }
    return next;
}



isolated function processResourceBoundSearchParameter(FHIRSearchParameterDefinition definition,
                                                        string fhirResourceType,
                                                        readonly & SearchParamConfig config,
                                                        RequestQueryParameter queryParam) 
                                                                        returns RequestSearchParameter[]|FHIRError {
    SearchParameterPreProcessor? customPreprocessor = config.preProcessor;
    if customPreprocessor != () {
        // If integration developer has registered a custom preprocessor
        return check customPreprocessor(definition, fhirResourceType, queryParam);
    } else {
        return check preprocessGeneralSearchParameter(definition, queryParam);
    }
}


isolated function processCommonSearchParameter(CommonSearchParameterDefinition definition, 
                                                string fhirResourceType,
                                                RequestQueryParameter queryParam, 
                                                ResourceAPIConfig apiConfig, 
                                                map<SearchParamConfig> & readonly searchParamConfigMap) 
                                                                        returns RequestSearchParameter[]|FHIRError {
    if searchParamConfigMap.hasKey(definition.name) {
        // The common search parameter behavior is override via Resource API Config
        SearchParamConfig & readonly paramConfig = searchParamConfigMap.get(definition.name);
        if paramConfig.active {
            //Check whether preprocessor is overriden
            readonly & SearchParameterPreProcessor? customPreprocessor = paramConfig?.preProcessor;
            if customPreprocessor != () {
                // Create temporary FHIR Search parameter definition from common search parameter definition
                FHIRSearchParameterDefinition tempParamDef = {
                    name: definition.name,
                    'type: definition.'type,
                    base: definition.base,
                    expression: definition.expression
                };
                return check customPreprocessor(tempParamDef, fhirResourceType, queryParam);
            }
        } else {
            string diagnose = "Unsupported search parameter \"" + queryParam.name + "\" for resource type \""+ 
                                fhirResourceType +"\". Supported search parameters are: " +
                                 extractActiveSearchParameterNames(searchParamConfigMap).toBalString();
            return createFHIRError("Unsupported search parameter : " + queryParam.name, ERROR, PROCESSING_NOT_SUPPORTED, 
                                            diagnostic = diagnose, httpStatusCode = http:STATUS_NOT_IMPLEMENTED);
        }
    }
    // If we reach here, the developer haven't override the search parameter
    CommonSearchParameterPreProcessor? preProcessor = definition.preProcessor;
    if preProcessor != () {
        // Execute dedicated pre-processor
        return check preProcessor(definition, queryParam, apiConfig);       
    } else {
        // general type based pre precessing
        return check preprocessGeneralSearchParameter(definition, queryParam);
    }
}

isolated function getCommonSearchParamDefault(CommonSearchParameterDefinition definition, ResourceAPIConfig apiConfig) 
                                                                        returns RequestSearchParameter|FHIRError?{

    anydata value;
    anydata|SearchParameterDefaultValueProcessor? default = definition.default;
    if default != () {
        if default is SearchParameterDefaultValueProcessor {
            SearchParameterDefaultValueProcessor defaultFn = <SearchParameterDefaultValueProcessor>default;
            value = check defaultFn(definition, apiConfig);
        } else {
            value = default;
        }
        return check createRequestSearchParameter(definition, (), value);
    }
    return ();
}

isolated function preprocessGeneralSearchParameter(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition,
                                            RequestQueryParameter queryParam) returns RequestSearchParameter[]|FHIRError {
    RequestSearchParameter[] parameters = [];
    foreach string value in queryParam.values {
        RequestSearchParameter? requestSearchParam = 
                                            check createRequestSearchParameter(definition, queryParam.modifier, value);
        if requestSearchParam != () {
            parameters.push(requestSearchParam);
        }
    }
    return parameters;
}




// Private functions

# Function to validate FHIR request in HTTP level
#
# + request - HTTP request object
# + return - FHIRRequestMimeHeaders containing details extracted from about MIME types. FHIRError otherwise
isolated function validateClientRequestHeaders(http:Request request) returns FHIRRequestMimeHeaders|FHIRError {
    FHIRRequestMimeHeaders headers = {};
    string contentType = request.getContentType();

    match contentType {
        "" => {
            // Accept since it is not mandatory
        }
        "application/fhir+json" => {
            headers.contentType = JSON;
        }
        "application/fhir+xml" => {
            headers.contentType = XML; 
        }
        _ => {
            string message = "Incorrect Content-Type header value of \"" + contentType +
                             "\" was provided in the request. A valid FHIR Content-Type (\"application/fhir+json\" " +
                             "or \"application/fhir+xml\") is required.";
            return createFHIRError(message, ERROR, PROCESSING, message, 
                                    errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    string|http:HeaderNotFoundError acceptHeader = request.getHeader("Accept");
    if acceptHeader is string {
        match acceptHeader {
            "" => {
                // Accept since it is not mandatory
            }
            "*/*" => {
                // Client accepts anything, go with the default
            }
            "application/fhir+json" => {
                headers.acceptType = JSON;
            }
            "application/fhir+xml" => {
                headers.acceptType = XML;
            }
            _ => {
                string message = "Incorrect Accept header value of \"" + acceptHeader +
                                "\" was provided in the request. A valid FHIR Accept (\"application/fhir+json\" " +
                                "or \"application/fhir+xml\") type is required.";
                return createFHIRError(message, ERROR, PROCESSING, message, 
                                        errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_NOT_ACCEPTABLE);
            }
        }
    }
    

    return headers;
}

isolated function getJwtDetails(http:Request httpRequest) returns readonly & FHIRSecurity|FHIRError {

    readonly & FHIRSecurity fhirSecurity;
    string|error jwt = httpRequest.getHeader("x-jwt-assertion");
    if (jwt is string) {
        [jwt:Header, jwt:Payload]|error headerPayload = jwt:decode(jwt);
        if (headerPayload is [jwt:Header, jwt:Payload]) {
            [jwt:Header, jwt:Payload] [header, payload] = headerPayload;
            readonly & JWT jwtInfo = {
                header: header.cloneReadOnly(),
                payload: payload.cloneReadOnly()
            };
            if(payload.hasKey("idp_claims")){
                json idp_claims = <json>payload.get("idp_claims");
                map<string>|error claimList = idp_claims.fromJsonWithType();
                if (claimList is error) {
                    string message = "IDP claims are not available";
                    return createFHIRError(message, ERROR, PROCESSING, message,
                                            errorType = PROCESSING_ERROR, httpStatusCode = http:STATUS_UNAUTHORIZED);
                }
                // Split the scope string
                string[] scopeslist = regex:split(<string>payload.get("scope"), " ");
                json|error userName = idp_claims.username;
                if (userName is error) {
                    string message = "Username is not available";
                    return createFHIRError(message, ERROR, PROCESSING, message,
                                            errorType = PROCESSING_ERROR, httpStatusCode = http:STATUS_UNAUTHORIZED);
                } else {
                    readonly & FHIRUser fhirUserInfo = {
                        userID: <string & readonly> userName.toString(),
                        scopes: <string[] & readonly> scopeslist.cloneReadOnly(),
                        claims: <map<string> & readonly> claimList.cloneReadOnly()
                    };
                    fhirSecurity ={
                        securedAPICall:true,
                        fhirUser: fhirUserInfo,
                        jwt: jwtInfo
                    };
                    return fhirSecurity;
                }

            } else {
                    fhirSecurity ={
                        securedAPICall:true,
                        jwt: jwtInfo,
                        fhirUser: ()
                    };
                    return fhirSecurity;
            }
        } else {
            string message = "Error occured in JWT decode";
            return createFHIRError(message, ERROR, PROCESSING, message,
                                    errorType = PROCESSING_ERROR, httpStatusCode = http:STATUS_UNAUTHORIZED);
        }
    } else {
        fhirSecurity ={
            securedAPICall:false,
            jwt: (),
            fhirUser: ()
        };
        return fhirSecurity;
    }
}

isolated function getFHIRSecurity(http:Request request) returns readonly & FHIRSecurity | FHIRError {

    return getJwtDetails(request);
}

isolated function createHTTPRequestRecord(http:Request request, json|xml? payload) returns readonly & HTTPRequest {
    map<string[]> headers = {};
    foreach string headerName in request.getHeaderNames() {
        string[]|http:HeaderNotFoundError headerResult = request.getHeaders(headerName);
        if headerResult is string[] {
            headers[headerName] = headerResult;
        }
    }

    return {
        headers: headers.cloneReadOnly(),
        payload: payload.cloneReadOnly()
    };
}
