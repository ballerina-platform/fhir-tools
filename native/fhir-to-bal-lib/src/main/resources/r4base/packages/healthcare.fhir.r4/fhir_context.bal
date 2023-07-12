// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# FHIR Context class: used to transfer FHIR request related information down integration flow
public isolated class FHIRContext {
    private MessageDirection direction = IN;
    private final FHIRRequest fhirRequest;
    private final readonly & FHIRSecurity fhirSecurity;
    private final readonly & HTTPRequest httpRequest;
    private FHIRResponse|FHIRContainerResponse? fhirResponse = ();

    isolated function init(FHIRRequest request, readonly & HTTPRequest httpRequest, readonly & FHIRSecurity security) {
        self.fhirRequest = request;
        self.httpRequest = httpRequest;
        self.fhirSecurity = security;
    }

    # Set context direction : indicate whether the request is in request direction or response direction
    #
    # + direction - message direction (IMPORTANT : This is used for internal processing hence adviced not to update)
    isolated function setDirection(MessageDirection direction) {
        lock {
            self.direction = direction;
        }
    }

    # Get context direction : indicate whether the request is in request direction or response direction
    #
    # + return - Message direction
    public isolated function getDirection() returns MessageDirection {
        lock {
            return self.direction;
        }
    }

    # Get FHIR request
    # FHIR request parsed information about incoming FHIR request from the client application
    #
    # + return - FHIR request object
    public isolated function getFHIRRequest() returns FHIRRequest? {
        return self.fhirRequest;
    }

    # Get FHIR security information
    # FHIR request derived security information about incoming FHIR request from the client application
    #
    # + return - FHIR security record
    public isolated function getFHIRSecurity() returns FHIRSecurity? {
        return self.fhirSecurity;
    }

    # Get incoming raw HTTP request information
    #
    # + return - Incoming HTTP request
    public isolated function getHTTPRequest() returns HTTPRequest? {
        return self.httpRequest;
    }

    # Set FHIR response sent to client application
    #
    # + response - FHIR response message
    isolated function setFHIRResponse(FHIRResponse|FHIRContainerResponse response) {
        lock {
            self.fhirResponse = response;
        }
    }

    # Get FHIR response
    #
    # + return - FHIR response
    public isolated function getFHIRResponse() returns FHIRResponse|FHIRContainerResponse? {
        lock {
            return self.fhirResponse;
        }
    }

    # Get FHIR interaction information parsed from the incoming FHIR request
    # http://hl7.org/fhir/http.html#3.1.0
    #
    # + return - FHIR interaction record
    public isolated function getInteraction() returns FHIRInteraction {
        return self.fhirRequest.getInteraction();
    }

    # Get target FHIR resource type
    #
    # + return - FHIR resource type
    public isolated function getResourceType() returns string? {
        return self.fhirRequest.getResourceType();
    }

    # Get client accepted response format
    #
    # + return - Client accepted FHIR payload format
    public isolated function getClientAcceptFormat() returns FHIRPayloadFormat {
        return self.fhirRequest.getClientAcceptFormat();
    }

    # Get FHIR User : End user information available in the JWT
    #
    # + return - FHIR Usr information
    public isolated function getFHIRUser() returns readonly & FHIRUser? {
        return self.fhirSecurity.fhirUser;
    }

    # Get all request parameters
    # 
    # + return - Request search parameter map (Key of the map is name of the search parameter).
    public isolated function getRequestSearchParameters() returns readonly & map<readonly & RequestSearchParameter[]> {
        return self.fhirRequest.getSearchParameters();
    }

    # Get search parameter with given name
    # 
    # + name - Name of the search parameter
    # + return - Request search parameter array if available. Otherwise nil
    public isolated function getRequestSearchParameter(string name) returns readonly & RequestSearchParameter[]? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            return searchParameters.get(name);
        }
        return ();
    }

    # Function to get Number typed search parameter with given name from the FHIR API request
    # 
    # + name - Name of the search parameter
    # + return - Search parameter if exists, nil if not found and FHIRTypeError if search parameter is not Number type
    public isolated function getNumberSearchParameter(string name) returns NumberSearchParameter[]|FHIRTypeError? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            NumberSearchParameter[] paramArray = [];
            foreach readonly & RequestSearchParameter param in searchParameters.get(name) {
                FHIRTypedSearchParameter typedValue = param.typedValue;
                if typedValue is NumberSearchParameter {
                    paramArray.push(typedValue);
                } else {
                    string msg = "Search parameter type mismatch";
                    string diagMsg = "FHIR Search parameter with name : " + name + " is not a Number type search parameter." + 
                                        " Search parameter type : " + (typeof param).toBalString();
                    return <FHIRTypeError>createInternalFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, errorType = TYPE_ERROR);
                }           
            }
            return paramArray;
        }
        return ();
    }

    # Function to get Reference typed search parameter with given name from the FHIR API request   
    # 
    # + name - Name of the search parameter
    # + return - Search parameter if exists, nil if not found and FHIRTypeError if search parameter is not Reference type
    public isolated function getReferenceSearchParameter(string name) returns ReferenceSearchParameter[]|FHIRTypeError? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            ReferenceSearchParameter[] paramArray = [];
            foreach readonly & RequestSearchParameter param in searchParameters.get(name) {
                FHIRTypedSearchParameter typedValue = param.typedValue;
                if typedValue is ReferenceSearchParameter {
                    paramArray.push(typedValue);
                } else {
                    string msg = "Search parameter type mismatch";
                    string diagMsg = "FHIR Search parameter with name : " + name + " is not a Reference type search parameter." + 
                                        " Search parameter type : " + (typeof param).toBalString();
                    return <FHIRTypeError>createInternalFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, errorType = TYPE_ERROR);
                }           
            }
            return paramArray;
        }
        return ();
    }

    # Function to get String typed search parameter with given name from the FHIR API request   
    # 
    # + name - Name of the search parameter
    # + return - Search parameter if exists, nil if not found and FHIRTypeError if search parameter is not String type
    public isolated function getStringSearchParameter(string name) returns StringSearchParameter[]|FHIRTypeError? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            StringSearchParameter[] paramArray = [];
            foreach readonly & RequestSearchParameter param in searchParameters.get(name) {
                FHIRTypedSearchParameter typedValue = param.typedValue;
                if typedValue is StringSearchParameter {
                    paramArray.push(typedValue);
                } else {
                    string msg = "Search parameter type mismatch";
                    string diagMsg = "FHIR Search parameter with name : " + name + " is not a String type search parameter." + 
                                        " Search parameter type : " + (typeof param).toBalString();
                    return <FHIRTypeError>createInternalFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, errorType = TYPE_ERROR);
                }           
            }
            return paramArray;
        }
        return ();
    }

    # Function to get Token typed search parameter with given name from the FHIR API request   
    # 
    # + name - Name of the search parameter
    # + return - Search parameter if exists, nil if not found and FHIRTypeError if search parameter is not Token type
    public isolated function getTokenSearchParameter(string name) returns TokenSearchParameter[]|FHIRTypeError? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            TokenSearchParameter[] paramArray = [];
            foreach readonly & RequestSearchParameter param in searchParameters.get(name) {
                FHIRTypedSearchParameter typedValue = param.typedValue;
                if typedValue is TokenSearchParameter {
                    paramArray.push(typedValue);
                } else {
                    string msg = "Search parameter type mismatch";
                    string diagMsg = "FHIR Search parameter with name : " + name + " is not a Token type search parameter." + 
                                        " Search parameter type : " + (typeof param).toBalString();
                    return <FHIRTypeError>createInternalFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, errorType = TYPE_ERROR);
                }           
            }
            return paramArray;
        }
        return ();
    }

    # Function to get URI typed search parameter with given name from the FHIR API request   
    # 
    # + name - Name of the search parameter
    # + return - Search parameter if exists, nil if not found and FHIRTypeError if search parameter is not URI type
    public isolated function getURISearchParameter(string name) returns URISearchParameter[]|FHIRTypeError? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            URISearchParameter[] paramArray = [];
            foreach readonly & RequestSearchParameter param in searchParameters.get(name) {
                FHIRTypedSearchParameter typedValue = param.typedValue;
                if typedValue is URISearchParameter {
                    paramArray.push(typedValue);
                } else {
                    string msg = "Search parameter type mismatch";
                    string diagMsg = "FHIR Search parameter with name : " + name + " is not a URI type search parameter." + 
                                        " Search parameter type : " + (typeof param).toBalString();
                    return <FHIRTypeError>createInternalFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, errorType = TYPE_ERROR);
                }           
            }
            return paramArray;
        }
        return ();
    }

    # Function to get Date typed search parameter with given name from the FHIR API request   
    # 
    # + name - Name of the search parameter
    # + return - Search parameter if exists, nil if not found and FHIRTypeError if search parameter is not Date type
    public isolated function getDateSearchParameter(string name) returns DateSearchParameter[]|FHIRTypeError? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            DateSearchParameter[] paramArray = [];
            foreach readonly & RequestSearchParameter param in searchParameters.get(name) {
                FHIRTypedSearchParameter typedValue = param.typedValue;
                if typedValue is DateSearchParameter {
                    paramArray.push(typedValue);
                } else {
                    string msg = "Search parameter type mismatch";
                    string diagMsg = "FHIR Search parameter with name : " + name + " is not a Date type search parameter." + 
                                        " Search parameter type : " + (typeof param).toBalString();
                    return <FHIRTypeError>createInternalFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, errorType = TYPE_ERROR);
                }           
            }
            return paramArray;
        }
        return ();
    }

    # Function to get Quantity typed search parameter with given name from the FHIR API request   
    # 
    # + name - Name of the search parameter
    # + return - Search parameter if exists, nil if not found and FHIRTypeError if search parameter is not Quantity type
    public isolated function getQuantitySearchParameter(string name) returns QuantitySearchParameter[]|FHIRTypeError? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            QuantitySearchParameter[] paramArray = [];
            foreach readonly & RequestSearchParameter param in searchParameters.get(name) {
                FHIRTypedSearchParameter typedValue = param.typedValue;
                if typedValue is QuantitySearchParameter {
                    paramArray.push(typedValue);
                } else {
                    string msg = "Search parameter type mismatch";
                    string diagMsg = "FHIR Search parameter with name : " + name + " is not a Quantity type search parameter." + 
                                        " Search parameter type : " + (typeof param).toBalString();
                    return <FHIRTypeError>createInternalFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, errorType = TYPE_ERROR);
                }           
            }
            return paramArray;
        }
        return ();
    }

    # Function to get Composite typed search parameter with given name from the FHIR API request   
    # 
    # + name - Name of the search parameter
    # + return - Search parameter if exists, nil if not found and FHIRTypeError if search parameter is not Composite type
    public isolated function getCompositeSearchParameter(string name) returns CompositeSearchParameter[]|FHIRTypeError? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            CompositeSearchParameter[] paramArray = [];
            foreach readonly & RequestSearchParameter param in searchParameters.get(name) {
                FHIRTypedSearchParameter typedValue = param.typedValue;
                if typedValue is CompositeSearchParameter {
                    paramArray.push(typedValue);
                } else {
                    string msg = "Search parameter type mismatch";
                    string diagMsg = "FHIR Search parameter with name : " + name + " is not a Composite type search parameter." + 
                                        " Search parameter type : " + (typeof param).toBalString();
                    return <FHIRTypeError>createInternalFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, errorType = TYPE_ERROR);
                }           
            }
            return paramArray;
        }
        return ();
    }

    # Function to get Special typed search parameter with given name from the FHIR API request   
    # 
    # + name - Name of the search parameter
    # + return - Search parameter if exists, nill if not found and FHIRTypeError if search parameter is not Special type 
    public isolated function getSpecialSearchParameter(string name) returns SpecialSearchParameter[]|FHIRTypeError? {
        map<RequestSearchParameter[] & readonly> & readonly searchParameters = self.fhirRequest.getSearchParameters();
        if searchParameters.hasKey(name) {
            SpecialSearchParameter[] paramArray = [];
            foreach readonly & RequestSearchParameter param in searchParameters.get(name) {
                FHIRTypedSearchParameter typedValue = param.typedValue;
                if typedValue is SpecialSearchParameter {
                    paramArray.push(typedValue);
                } else {
                    string msg = "Search parameter type mismatch";
                    string diagMsg = "FHIR Search parameter with name : " + name + " is not a Special type search parameter." + 
                                        " Search parameter type : " + (typeof param).toBalString();
                    return <FHIRTypeError>createInternalFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, errorType = TYPE_ERROR);
                }           
            }
            return paramArray;
        }
        return ();
    }
}
