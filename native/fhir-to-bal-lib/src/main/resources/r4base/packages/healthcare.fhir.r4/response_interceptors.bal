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
import ballerina/uuid;

# Response error interceptor to post-process FHIR responses
public isolated service class FHIRResponseInterceptor {
    *http:ResponseInterceptor;

    final ResourceAPIConfig apiConfig;
    private final readonly & map<SearchParamConfig> searchParamConfigMap;

    public function init(ResourceAPIConfig apiConfig) {
        self.apiConfig = apiConfig;

        map<SearchParamConfig> searchParamConfigs = {};
        // process seach parameters
        foreach SearchParamConfig item in self.apiConfig.searchParameters {
            if item.active {
                searchParamConfigs[item.name] = item;
            }
        }
        self.searchParamConfigMap = searchParamConfigs.cloneReadOnly();
    }
        
    remote isolated function interceptResponse(http:RequestContext ctx, http:Response res) returns http:NextService|FHIRError? {
        log:printDebug("Execute: FHIR Response Interceptor");
        FHIRContext fhirContext = check getFHIRContext(ctx);
        fhirContext.setDirection(OUT);

        check self.postProcessSearchParameters(fhirContext);

        FHIRResponse|FHIRContainerResponse? fhirResponse = fhirContext.getFHIRResponse();
        FHIRPayloadFormat acceptFormat = fhirContext.getClientAcceptFormat();
        if fhirResponse != () {
            if acceptFormat is JSON {
                json jsonResult;
                if fhirResponse is FHIRResponse {
                    jsonResult = check fhirResponse.getResourceEntity().toJson();
                } else {
                    jsonResult = check (<FHIRContainerResponse>fhirResponse).getResourceEntity().toJson();
                }
                res.setJsonPayload(jsonResult, FHIR_MIME_TYPE_JSON);
            } else {
                xml xmlResult;
                if fhirResponse is FHIRResponse {
                    xmlResult = check fhirResponse.getResourceEntity().toXml();
                } else {
                    xmlResult = check (<FHIRContainerResponse>fhirResponse).getResourceEntity().toXml();
                }
                res.setXmlPayload(xmlResult, FHIR_MIME_TYPE_XML);
            }
        }
        return getNextService(ctx);
    }

    isolated function postProcessSearchParameters(FHIRContext context) returns FHIRError? {
        map<RequestSearchParameter[]> & readonly searchParameters = context.getRequestSearchParameters();

        foreach string paramName in searchParameters.keys() {
            RequestSearchParameter[] searchParams = searchParameters.get(paramName);
            foreach RequestSearchParameter searchParam in searchParams {
                if self.searchParamConfigMap.hasKey(paramName) {
                    SearchParamConfig & readonly paramConfig = self.searchParamConfigMap.get(paramName);
                    SearchParameterPostProcessor? postProcessor = paramConfig.postProcessor;
                    if postProcessor != () {
                        FHIRSearchParameterDefinition? definition = fhirRegistry.getResourceSearchParameterByName(self.apiConfig.resourceType, paramName);
                        if definition != () {
                            check postProcessor(definition, searchParam, context);
                        }
                    }
                } else if COMMON_SEARCH_PARAMETERS.hasKey(paramName) {
                    // post process common search parameter
                    CommonSearchParameterDefinition & readonly definition = COMMON_SEARCH_PARAMETERS.get(paramName);
                    CommonSearchParameterPostProcessor? & readonly postProcessor = definition.postProcessor;
                    if postProcessor != () {
                        check postProcessor(definition, searchParam, context);
                    }
                }
            }
        }
    }
   
}

# Response error interceptor to handle errors in resource and response flow
public isolated service class FHIRResponseErrorInterceptor {
   *http:ResponseErrorInterceptor;
 
   remote isolated function interceptResponseError(error err) returns http:StatusCodeResponse {
        log:printDebug("Execute: FHIR Response Error Interceptor");
        
        // TODO Implement error handling here [https://github.com/wso2-enterprise/open-healthcare/issues/891]
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
