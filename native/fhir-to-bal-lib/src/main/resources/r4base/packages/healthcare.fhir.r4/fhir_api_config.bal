// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Function type to be implemented to override the operation pre-processing
public type OperationPreProcessor function (FHIROperationDefinition definition, FHIRContext context) returns FHIRError?;
# Function type to be implemented to override the operation post-processing
public type OperationPostProcessor function (FHIROperationDefinition definition, FHIRContext context) returns FHIRError?;


# Redefined FHIR read-only FHIR resource API config
public type ResourceAPIConfig readonly & ResourceAPIConfigType;

# API Config representation
#
# + resourceType - FHIR resource type of the API  
# + profiles - profiles supported by the API
# + defaultProfile - default profile that the FHIR API is supporting
# + searchParameters - Search parameters supported by the FHIR API 
# + operations - Operations supported by the FHIR API
# + serverConfig - Serevr configuration
public type ResourceAPIConfigType record {|
    readonly string resourceType;
    readonly string[] profiles;
    readonly string? defaultProfile;
    readonly SearchParamConfig[] searchParameters;
    readonly OperationConfig[] operations;
    readonly ServerConfig? serverConfig;
|};

# Search parameter configuration
#
# + name - Name of the search parameter  
# + active - Is this search parameter is activated or deactivated  
# + preProcessor - Override this search parameter pre-processing function. If the integration  developer wants 
#                   to take control of pre-processing the search parameter.  
# + postProcessor - Override this search parameter post-processing function. If the integration  developer wants 
#                   to take control of post-processing the search parameter
# + information - Meta infomation about the search parameter (no processed, just for information)
public type SearchParamConfig record {|
    readonly string name;
    readonly boolean active;
    readonly & SearchParameterPreProcessor preProcessor?;
    readonly & SearchParameterPostProcessor postProcessor?;
    readonly Information information?;
|};

# Operation configuration
#
# + name - Name of the operation
# + active - Is this operation is activated or deactivated
# + preProcessor - Override this operation pre-processing function. If the integration  developer wants to take 
#                       control of pre-processing the operation.  
# + postProcessor - Override this operation post-processing function. If the integration  developer wants to take 
#                       control of post-processing the operation.
# + information - Meta infomation about the operation (no processed, just for information)
public type OperationConfig record {|
    readonly string name;
    readonly boolean active;
    readonly & OperationPreProcessor preProcessor?;
    readonly & OperationPostProcessor postProcessor?;
    readonly Information information?;
|};

# Information about a rest feature
#
# + description - Description
# + builtin - Is this feature is available as a built-in feature
# + documentation - Documentation link
public type Information record {|
    readonly string description;
    readonly boolean builtin?;
    readonly string documentation?;
|};

# FHIR Server configurations
#
# + apis - list of FHIR APIs that is implemented in the organization
public type ServerConfig record {|
    readonly map<ApiInfo> apis;
|};

# FHIR API information
#
# + resourceType - FHIR resource type
# + searchParameters - supported search parameters
public type ApiInfo record {|
    readonly string resourceType;
    readonly string[] searchParameters;
|};
