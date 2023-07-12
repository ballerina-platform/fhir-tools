// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

import ballerina/lang.value;
import ballerina/http;

public isolated function getFHIRContext(http:RequestContext httpCtx) returns FHIRContext|FHIRError {
    value:Cloneable|object {} fhirCtx = httpCtx.get(FHIR_CONTEXT_PROP_NAME);
    if fhirCtx is FHIRContext {
        return fhirCtx;
    }
    string diag = "Unable to find FHIR context in HTTP request context.";
    return createInternalFHIRError("FHIR Context not found", FATAL, PROCESSING_NOT_FOUND, diagnostic = diag);
}

public isolated function getRequestResourceEntity(http:RequestContext ctx) returns FHIRResourceEntity|FHIRError {

    FHIRContext fhirCtx = check getFHIRContext(ctx);
    FHIRRequest? request = fhirCtx.getFHIRRequest();
    if request is FHIRRequest {
        FHIRResourceEntity? resourceEntity = request.getResourceEntity();
        if resourceEntity is FHIRResourceEntity {
            return resourceEntity;
        }
    }
    return createFHIRError("FHIR Request payload not found", ERROR, PROCESSING_NOT_FOUND);
}



public isolated function setResponseResourceEntity(FHIRResourceEntity|FHIRContainerResourceEntity entity, 
                                                                            http:RequestContext ctx) returns FHIRError? {
    FHIRContext fhirCtx = check getFHIRContext(ctx);
    
    if entity is FHIRResourceEntity {
        FHIRResponse response = new(entity);
        fhirCtx.setFHIRResponse(response);
    } else {
        FHIRContainerResponse response = new(<FHIRContainerResourceEntity>entity);
        fhirCtx.setFHIRResponse(response);
    }
    
}


isolated function getResourceDefinition(typedesc resourceType) returns ResourceDefinitionRecord|FHIRTypeError {
    ResourceDefinitionRecord? def = resourceType.@ResourceDefinition;
    if def != () {
        return def;
    } else {
        string message = "Provided type does not represent a FHIR resource";
        string diagnostic = "Unable to find resource definition of given resource of type :" + resourceType.toBalString();
        return <FHIRTypeError> createInternalFHIRError(message, FATAL, PROCESSING, diagnostic = diagnostic);
    }
}

# Utility function to create request search parameter record
# 
# + name - name of the search parameter
# + paramType - Search parameter type
# + originalValue - Original incoming search parameter value
# + typedValue - Typed (parsed/decoded) search parameter record
# + return - Created RequestSearchParameter
isolated function createSearchParameterWrapper(string & readonly name, FHIRSearchParameterType & readonly paramType, 
                                        string & readonly originalValue, FHIRTypedSearchParameter & readonly typedValue) 
                                                                                        returns RequestSearchParameter {
    RequestSearchParameter searchParam = {
        'type: paramType,
        name: name,
        value: originalValue,
        typedValue: typedValue
    };
    return searchParam;
}
