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
import ballerina/regex;
import ballerina/time;
import ballerina/log;

# Function type to be implemented to override the search parameter pre-processing
public type SearchParameterPreProcessor isolated function (FHIRSearchParameterDefinition definition, 
                                                            string resourceType, 
                                                            RequestQueryParameter querrParam) 
                                                                returns RequestSearchParameter[]|FHIRError;

# Function type to be implemented to override the search parameter post-processing
public type SearchParameterPostProcessor isolated function (FHIRSearchParameterDefinition definition, 
                                                                RequestSearchParameter requestSearchParam, 
                                                                FHIRContext context) 
                                                                    returns FHIRError?;

# common search parameter pre processor function
type CommonSearchParameterPreProcessor isolated function (CommonSearchParameterDefinition definition, 
                                                            RequestQueryParameter queryParam, 
                                                            ResourceAPIConfig apiConfig) 
                                                                returns RequestSearchParameter[]|FHIRError;
# common search parameter post processor function
type CommonSearchParameterPostProcessor isolated function (CommonSearchParameterDefinition definition, 
                            RequestSearchParameter requestSearchParam, FHIRContext context) returns FHIRError?;

type SearchParameterDefaultValueProcessor isolated function 
                                (CommonSearchParameterDefinition definition, ResourceAPIConfig apiConfig) returns anydata|FHIRError?;

# Represents incoming FHIR request search parameter
# + originalName - Original incoming request query param name
# + name - Name of the search parameter (FHIR)
# + modifier - Search parameter modifier
# + values - Values of the query parameter
public type RequestQueryParameter record {|
    string originalName;
    string name;
    FHIRSearchParameterModifier|string? modifier;
    string[] values;
|};

enum SearchParameterEffectiveLevel {
    SEARCH_PARAM_CATEGORY_RESOURCE_BOUND,
    SEARCH_PARAM_CATEGORY_RESOURCE_COMMON,
    SEARCH_PARAM_CATEGORY_SEARCH_RESULT,
    SEARCH_PARAM_CATEGORY_SPECIAL
}

# Common search parameter definition
#
# + default - Default value
# + multipleParams - Multiple occurences may happen in the request
# + effectiveLevel - Search parameter effective level
# + preProcessor - Field Description  
# + postProcessor - Field Description
type CommonSearchParameterDefinition record {
    *FHIRSearchParameterDefinition;
    anydata|SearchParameterDefaultValueProcessor? default = ();
    boolean multipleParams = false;
    SearchParameterEffectiveLevel effectiveLevel;
    CommonSearchParameterPreProcessor? preProcessor;
    CommonSearchParameterPostProcessor? postProcessor;
};


isolated function createRequestSearchParameter(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition, 
                                                FHIRSearchParameterModifier|string? modifier, anydata value) 
                                                                            returns RequestSearchParameter|FHIRError {
    match definition.'type {
        STRING => {
            // Search parameter is a simple string
            string strValue = value.toString();
            if modifier != () {
                check validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
            }
            StringSearchParameter & readonly sParameter = {
                name: definition.name,
                modifier: modifier,
                value: strValue
            };
            return createSearchParameterWrapper(definition.name, STRING, strValue, sParameter);
        }
        NUMBER => {
            // Search parameter SHALL be a number (a whole number, or a decimal).
            readonly & NumberSearchParameter numberResult = check parseNumber(definition, modifier, value);
            return createSearchParameterWrapper(definition.name, NUMBER, value.toString(), numberResult);
        }
        TOKEN => {
            // Search parameter on a coded element or identifier.
            readonly & TokenSearchParameter tokenResult = check parseToken(definition, modifier, value);
            return createSearchParameterWrapper(definition.name, TOKEN, value.toString(), tokenResult);
        }
        DATE => {
            // Search parameter is on a date/time. The date format is the standard XML format, though other formats may be supported.
            readonly & DateSearchParameter dateResult = check parseDate(definition, modifier, value);
            return createSearchParameterWrapper(definition.name, DATE, value.toString(), dateResult);
        }
        REFERENCE => {
            // A reference to another resource (Reference or canonical).
            readonly & ReferenceSearchParameter referenceResult = check parseRefernce(definition, modifier, value);
            return createSearchParameterWrapper(definition.name, REFERENCE, value.toString(), referenceResult);
        }
        QUANTITY => {
            readonly & QuantitySearchParameter quantityResult = check parseQuantity(definition, modifier, value);
            return createSearchParameterWrapper(definition.name, QUANTITY, value.toString(), quantityResult);
        }
        URI => {
            readonly & URISearchParameter parseURIResult = check parseURI(definition, modifier, value);
            return createSearchParameterWrapper(definition.name, URI, value.toString(), parseURIResult);
        }
        COMPOSITE => {
            readonly & CompositeSearchParameter compositeResult = check parseComposite(definition, modifier, value);
            return createSearchParameterWrapper(definition.name, COMPOSITE, value.toString(), compositeResult);
        }
        SPECIAL => {
            readonly & SpecialSearchParameter specialResult = check parseSpecial(definition, modifier, value);
            return createSearchParameterWrapper(definition.name, SPECIAL, value.toString(), specialResult);
        }
        _ => {
            string message = "Unknown search parameter type : " + definition.'type;
            return <FHIRValidationError> createInternalFHIRError(message, ERROR, PROCESSING, 
                                                                        errorType = VALIDATION_ERROR,
                                                                        diagnostic = message);
        }
    }
}

isolated function parseNumber(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition, 
                                                FHIRSearchParameterModifier|string? modifier, anydata value) 
                                                    returns readonly & NumberSearchParameter|FHIRValidationError {
    Prefix prefix;
    int|float decodedValue;
    if value is string {
        var [prefixResult, decodedValueStr] = decodePrefixedValue(value);
        log:printDebug(string `Decoded search parameter value : ${[prefixResult, decodedValueStr].toBalString()}`);
        int|error tempIntValue = int:fromString(decodedValueStr);
        if tempIntValue is int {
            // Number value is an integer
            decodedValue = tempIntValue;
        } else {
            // Number value may be a float
            float|error tempFloatValue = float:fromString(decodedValueStr);
            if tempFloatValue is float {
                // Number value is a float
                decodedValue = tempFloatValue;
            } else {
                string message = "Invalid value: \"" + value.toString() + 
                                    "\" for the search parameter: "  + definition.name;
                string diagMsg = "Int or Float value is expected for the search parameter : " + definition.name;
                return <FHIRValidationError>createFHIRError(message, ERROR, PROCESSING, diagnostic = diagMsg, 
                                        errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
            }
        }
        prefix = prefixResult;
    } else {
        // This happens when populating default values
        prefix = eq;
        int|error tempIntValue = value.ensureType();
        if tempIntValue is int {
            decodedValue = tempIntValue;
        } else {
            float|error tempFloatValue = value.ensureType();
            if tempFloatValue is float {
                decodedValue = tempFloatValue;
            } else {
                string msg = "Invalid value: \"" + value.toString() + 
                            "\" for the search parameter: " + definition.name;
                string diagMsg = "Int or Float value is expected for the search parameter : " + definition.name;
                return <FHIRValidationError>createFHIRError(msg, ERROR, PROCESSING, diagnostic = diagMsg, 
                                        errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
            }
        }
    }
    if modifier != () {
        check validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
    }

    readonly & NumberSearchParameter sParameter = {
        name: definition.name,
        modifier: modifier,
        value: decodedValue,
        prefix: prefix
    };
    return sParameter;
}

isolated function parseToken(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition, 
                                                FHIRSearchParameterModifier|string? modifier, anydata value) 
                                                    returns readonly & TokenSearchParameter|FHIRValidationError {
    if value is string {
        string[] split = regex:split(value.trim(), "\\|");
        string? system = ();
        string? code = ();
        int tokens = split.length();
        if tokens == 2 {
            // If the param value starts with "|" the split function returns an empty string in the resulting array
            //  Hence need  to check system value is not an empty string
            system = split[0].length() > 0 ? split[0] : ();
            code = split[1];
        } else if tokens == 1 {
            if !value.startsWith("|") {
                if !value.endsWith("|") {
                    code = split[0];
                } else {
                    system = split[0];
                }
            } else {
                code = split[0];
            }
        } else {
            // Invalid format
            string message = "Invalid value: \"" + value + "\" for the search parameter: " + definition.name;
            return <FHIRValidationError>createFHIRError(message, ERROR, PROCESSING, 
                                    diagnostic = message, 
                                    errorType = VALIDATION_ERROR, 
                                    httpStatusCode = http:STATUS_BAD_REQUEST);
        }
        // validate modifier
        if modifier != () {
            check validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
        }
        readonly & TokenSearchParameter token = {
            name: definition.name,
            modifier: modifier,
            system: system,
            code: code
        };
        return token;
    } else {
        string message = "Unsupported type : \"" + (typeof value).toBalString() +
                                    "\" for the search parameter: " + definition.name;
        return <FHIRValidationError>createFHIRError(message, ERROR, PROCESSING, 
                                diagnostic = message, 
                                errorType = VALIDATION_ERROR, 
                                httpStatusCode = http:STATUS_BAD_REQUEST);
    }
}

isolated function parseDate(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition, 
                                                FHIRSearchParameterModifier|string? modifier, anydata value)
                                                    returns readonly & DateSearchParameter|FHIRValidationError {
    
    if value is string {
        var [prefix, decodedValue] = decodePrefixedValue(value);
        time:Civil|error civilTime = iso8601toCivil(decodedValue);
        if civilTime is time:Civil {
            // validate modifier
            if modifier != () {
                check validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
            }
            readonly & DateSearchParameter dateParam = {
                name: definition.name,
                prefix: prefix,
                modifier: modifier,
                value: civilTime.cloneReadOnly()
            };
            return dateParam;
        } else {
            string message = "Error occurred while parsing search parameter : \"" + definition.name +
                                " with value : " + value.toBalString();
            string diagMsg = civilTime.message();
            return <FHIRValidationError>createFHIRError(message, ERROR, PROCESSING, 
                                                            diagnostic = diagMsg, 
                                                            errorType = VALIDATION_ERROR, 
                                                            httpStatusCode = http:STATUS_BAD_REQUEST);
        }
    } else {
        // Ideally are not reaching here
        string message = "Error occurred while parsing search parameter : \"" + definition.name +
                                " with value : " + value.toBalString();
        string diagMsg = "Unexpected type for the parameter value : " + (typeof value).toBalString();
        return <FHIRValidationError>createFHIRError(message, ERROR, PROCESSING, 
                                                        diagnostic = diagMsg, 
                                                        errorType = VALIDATION_ERROR, 
                                                        httpStatusCode = http:STATUS_BAD_REQUEST);
    }
}

isolated function parseRefernce(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition, 
                                                FHIRSearchParameterModifier|string? modifier, anydata value)
                                                    returns readonly & ReferenceSearchParameter|FHIRValidationError {
    if value is string {
        if value.indexOf("/") is () {
            // format : [parameter]=[id]
            if modifier != () {
                check validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
            }
            readonly & ReferenceSearchParameter refParam = {
                name: definition.name,
                resourceType: (),
                id: value,
                modifier: modifier,
                url: ()
            };
            return refParam;
        } else {
            string[] splitValues = regex:split(value, "/");
            if splitValues.length() == 2 {
                if fhirRegistry.isSupportedResource(splitValues[0]) {
                    //format : [parameter]=[type]/[id]
                    if modifier != () {
                        check validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
                    }
                    readonly & ReferenceSearchParameter refParam = {
                        name: definition.name,
                        resourceType: splitValues[0],
                        id: splitValues[1],
                        modifier: modifier,
                        url: ()
                    };
                    return refParam;
                } else {
                    // Unknown/Unsupported resource type
                    string message = "Unknown/Unsupported resource type : " + splitValues[0];
                    string diagMsg = "Search parameter : "  + definition.name + 
                                        " of type Reference. Detected format : [parameter]=[type]/[id], but unknown/unsupported type";
                    return <FHIRValidationError>createFHIRError(message, ERROR, PROCESSING, 
                                                                    diagnostic = diagMsg, 
                                                                    errorType = VALIDATION_ERROR, 
                                                                    httpStatusCode = http:STATUS_BAD_REQUEST);
                }
            } else {
                // format : [parameter]=[url]
                // Validate URL format
                boolean matches = regex:matches(value, CANONICAL_REGEX);
                log:printDebug(string `REGEX MATCH : ${matches.toBalString()}`);
                if modifier != () {
                    check validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
                }
                readonly & ReferenceSearchParameter refParam = {
                    name: definition.name,
                    resourceType: (),
                    id: (),
                    modifier: modifier,
                    url: value
                };
                return refParam;
            }
        }
    } else {
        // Ideally we are not reaching here
        string message = "Error occurred while parsing search parameter : \"" + definition.name +
                                " with value : " + value.toBalString();
        string diagMsg = "Unexpected type for the parameter value : " + (typeof value).toBalString();
        return <FHIRValidationError>createFHIRError(message, ERROR, PROCESSING, 
                                                        diagnostic = diagMsg, 
                                                        errorType = VALIDATION_ERROR, 
                                                        httpStatusCode = http:STATUS_BAD_REQUEST);
    }
}

isolated function parseQuantity(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition, 
                                                FHIRSearchParameterModifier|string? modifier, anydata value)
                                                    returns (QuantitySearchParameter & readonly)|FHIRParseError {

    if value is string {
        var [tempPrefix, strippedValue] = decodePrefixedValue(value);
        string[] valueComponents = regex:split(strippedValue, "\\|");

        Prefix? prefix = tempPrefix;
        string number;
        string? system = ();
        string? code = ();

        if valueComponents.length() > 0 {
            number = valueComponents[0];
        } else {
            string message = "Invalid value for the search parameter : " + definition.name;
            string diagMsg = "Number component missing in the value : " + value + 
                                ". Expected format is [parameter]=[prefix][number]|[system]|[code]";
            return <FHIRParseError>createFHIRError(message, ERROR, PROCESSING, diagnostic = diagMsg, 
                                    errorType = PARSE_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
        }
        if valueComponents.length() > 1 {
            system = valueComponents[1].length() > 0 ? valueComponents[1] : ();
        }

        if valueComponents.length() > 2 {
            code = valueComponents[2].length() > 0 ? valueComponents[2] : ();
        }

        int|float numberResult = check stringToNumber(number);

        if modifier != () {
            FHIRValidationError? validateModifierResult = validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
            if validateModifierResult is FHIRValidationError {
                return createParserErrorFrom(validateModifierResult);
            }
        }
        readonly & QuantitySearchParameter quantityParam = {
            name: definition.name,
            prefix: prefix,
            number: numberResult,
            system: system,
            code: code,
            modifier: ()
        };
        return quantityParam;

    } else {
        // Ideally are not reaching here
        string message = "Error occurred while parsing search parameter : \"" + definition.name +
                                " with value : " + value.toBalString();
        string diagMsg = "Unexpected type for the parameter value : " + (typeof value).toBalString();
        return <FHIRParseError>createFHIRError(message, ERROR, PROCESSING, 
                                                        diagnostic = diagMsg, 
                                                        errorType = PARSE_ERROR, 
                                                        httpStatusCode = http:STATUS_BAD_REQUEST);
    }
}

isolated function parseURI(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition, 
                                                FHIRSearchParameterModifier|string? modifier, anydata value)
                                                    returns readonly & URISearchParameter|FHIRParseError {
    if value is string {
        if modifier != () {
            FHIRValidationError? validateModifierResult = validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
            if validateModifierResult is FHIRValidationError {
                return createParserErrorFrom(validateModifierResult);
            }
        }
        readonly & URISearchParameter uriParam = {
            name: definition.name,
            modifier: modifier,
            uri: value
        };
        return uriParam;
    } else {
        // Ideally are not reaching here
        string message = "Error occurred while parsing search parameter : \"" + definition.name +
                                " with value : " + value.toBalString();
        string diagMsg = "Unexpected type for the parameter value : " + (typeof value).toBalString();
        return <FHIRParseError>createFHIRError(message, ERROR, PROCESSING, 
                                                        diagnostic = diagMsg, 
                                                        errorType = PARSE_ERROR, 
                                                        httpStatusCode = http:STATUS_BAD_REQUEST);
    }
}


isolated function parseComposite(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition, 
                                                FHIRSearchParameterModifier|string? modifier, anydata value)
                                                    returns readonly & CompositeSearchParameter|FHIRParseError {
    if value is string {
        if modifier != () {
            FHIRValidationError? validateModifierResult = validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
            if validateModifierResult is FHIRValidationError {
                return createParserErrorFrom(validateModifierResult);
            }
        }
        readonly & CompositeSearchParameter compositeParam = {
            name: definition.name,
            modifier: modifier,
            value: value
        };
        return compositeParam;
    } else {
        // Ideally are not reaching here
        string message = "Error occurred while parsing search parameter : \"" + definition.name +
                                " with value : " + value.toBalString();
        string diagMsg = "Unexpected type for the parameter value : " + (typeof value).toBalString();
        return <FHIRParseError>createFHIRError(message, ERROR, PROCESSING, 
                                                        diagnostic = diagMsg, 
                                                        errorType = PARSE_ERROR, 
                                                        httpStatusCode = http:STATUS_BAD_REQUEST);
    }
}

isolated function parseSpecial(FHIRSearchParameterDefinition|CommonSearchParameterDefinition definition, 
                                                FHIRSearchParameterModifier|string? modifier, anydata value)
                                                    returns readonly & SpecialSearchParameter|FHIRParseError {
    if value is string {
        if modifier != () {
            FHIRValidationError? validateModifierResult =
                                                validateModifier(modifier, <FHIRSearchParameterDefinition> definition);
            if validateModifierResult is FHIRValidationError {
                return createParserErrorFrom(validateModifierResult);
            }
        }
        readonly & SpecialSearchParameter specialParam = {
            name: definition.name,
            modifier: modifier,
            value: value
        };
        return specialParam;
    } else {
        // Ideally are not reaching here
        string message = "Error occurred while parsing search parameter : \"" + definition.name +
                                " with value : " + value.toBalString();
        string diagMsg = "Unexpected type for the parameter value : " + (typeof value).toBalString();
        return <FHIRParseError>createFHIRError(message, ERROR, PROCESSING, 
                                                        diagnostic = diagMsg, 
                                                        errorType = PARSE_ERROR, 
                                                        httpStatusCode = http:STATUS_BAD_REQUEST);
    }
}

isolated function stringToNumber(string value) returns int|float|FHIRParseError {
int|error tempIntValue = int:fromString(value);
    if tempIntValue is int {
        // Number value is an integer
        return tempIntValue;
    } else {
        // Number value may be a float
        float|error tempFloatValue = float:fromString(value);
        if tempFloatValue is float {
            // Number value is a float
            return tempFloatValue;
        } else {
            string message = "Failed to parse to an Integer or Float";
            string diagMsg = "Value : " + value + " cannot be parsed to an Integer or Float";
            return <FHIRParseError>createFHIRError(message, ERROR, PROCESSING, diagnostic = diagMsg, 
                                    errorType = PARSE_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
        }
    }
}


isolated function validateModifier(FHIRSearchParameterModifier|string modifier, FHIRSearchParameterDefinition definition) 
                                                                                    returns FHIRValidationError? {

    if modifier is FHIRSearchParameterModifier {
        if SEARCH_PARAM_MODIFIER_MAP.modifierTypeMapping.hasKey(modifier) {
            string[] & readonly paramTypes = SEARCH_PARAM_MODIFIER_MAP.modifierTypeMapping.get(modifier);
            if paramTypes.indexOf(definition.'type) != () {
                return;
            }
            string message = "Incompatible search parameter modifier";
            string diagMsg = "The modifier \"" + modifier + "\" is not compatible against parameter (\"" +
                                definition.name + "\") of type : " + definition.'type;
            return <FHIRValidationError> createFHIRError(message, ERROR, PROCESSING, 
                                                                    errorType = VALIDATION_ERROR,
                                                                    diagnostic = diagMsg);
        } 
        string message = "Unknown search parameter modifier";
        string diagMsg = "Incompatible modifier : \"" + modifier + "\" for search parameter : \"" + definition.name + "\"";
        return <FHIRValidationError> createFHIRError(message, ERROR, PROCESSING, 
                                                                errorType = VALIDATION_ERROR,
                                                                diagnostic = diagMsg);
    } else {
        if definition.'type == REFERENCE && fhirRegistry.isSupportedResource(modifier) {
            return;
        }
        string message = "Invalid search parameter modifier";
        string diagMsg = "Modifier :\"" + modifier + "\" of search parameter : \"" + definition.name + 
                            "\" must be a valid supported FHIR resource type";
        return <FHIRValidationError> createFHIRError(message, ERROR, PROCESSING, 
                                                                errorType = VALIDATION_ERROR,
                                                                diagnostic = diagMsg);
    }
}


isolated function paginationSearchParamPreProcessor(CommonSearchParameterDefinition definition, 
                                                        RequestQueryParameter query, ResourceAPIConfig apiConfig) 
                                                                            returns RequestSearchParameter[]|FHIRError {
    log:printDebug(string `Executing search parameter preprocessor for : ${definition.name}`);
    // We only support one occurrence of _offset and _count search parameter
    RequestSearchParameter resultParam = check createRequestSearchParameter(definition, query.modifier, query.values[0]);
    NumberSearchParameter|error numberParam = resultParam.typedValue.ensureType();
    if numberParam is NumberSearchParameter && numberParam.value is int {
        return [resultParam];
    } else {
        string message = "Invalid value for the search parameter: " + definition.name;
        string diagMsg = "The value: \"" + query.values[0] + "\" is invalid for the search parameter: " + definition.name +
                            ". It Expect to be an Integer.";
        return createFHIRError(message, ERROR, PROCESSING, diagnostic = diagMsg, 
                                errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
    }
}

isolated function  paginationSearchParamPostProcessor(CommonSearchParameterDefinition definition, 
                                    RequestSearchParameter requestSearchParam, FHIRContext context) returns FHIRError? {
    log:printDebug("Executing search parameter postprocessor for : _count");
    // nothing perform
    return ();
}


isolated function  _profileSearchParamPreProcessor(CommonSearchParameterDefinition definition, 
                                                    RequestQueryParameter query, ResourceAPIConfig apiConfig) 
                                                                            returns RequestSearchParameter[]|FHIRError {
    log:printDebug(string `Executing search parameter preprocessor for: ${definition.name}`);
    // We only support one occurrence of _profile search parameter ATM
    string profileStr = query.values[0];
    string[] & readonly profiles = apiConfig.profiles;
    int? indexOf = profiles.indexOf(profileStr);
    if indexOf is () {
        string message = "Unsupported profile: \"" + profileStr + "\" for the resource: "  + apiConfig.resourceType + 
                            ". Supported profiles are: " + apiConfig.profiles.toString();
        return createFHIRError(message, ERROR, PROCESSING, diagnostic = message, 
                                    errorType = VALIDATION_ERROR, httpStatusCode = http:STATUS_BAD_REQUEST);
    }

    URISearchParameter & readonly param = {
        name: definition.name,
        modifier: (),
        uri: profileStr
    };
    RequestSearchParameter wrappedParam = createSearchParameterWrapper(definition.name, URI, profileStr, param);
    return [wrappedParam];
}

// Function to decode search parameter key and return the name and modifier
isolated function decodeSearchParameterKey(string paramName, string[] values) returns RequestQueryParameter|FHIRError {
    string[] paramSplitResult = regex:split(paramName, ":");
    string qParamName = paramSplitResult[0];
    FHIRSearchParameterModifier|string? modifier = ();
    if paramSplitResult.length() > 1 {
        modifier = paramSplitResult[1];
        match paramSplitResult[1] {
            MODIFIER_ABOVE => {
                modifier = MODIFIER_ABOVE;
            }
            MODIFIER_BELOW => {
                modifier = MODIFIER_BELOW;
            }
            MODIFIER_CODE_TEXT => {
                modifier = MODIFIER_CODE_TEXT;
            }
            MODIFIER_CONTAINS => {
                modifier = MODIFIER_CONTAINS;
            }
            MODIFIER_EXACT => {
                modifier = MODIFIER_EXACT;
            }
            MODIFIER_IDENTIFIER => {
                modifier = MODIFIER_IDENTIFIER;
            }
            MODIFIER_IN => {
                modifier = MODIFIER_IN;
            }
            MODIFIER_ITERATE => {
                modifier = MODIFIER_ITERATE;
            }
            MODIFIER_MISSING => {
                modifier = MODIFIER_MISSING;
            }
            MODIFIER_NOT => {
                modifier = MODIFIER_NOT;
            }
            MODIFIER_NOT_IN => {
                modifier = MODIFIER_NOT_IN;
            }
            MODIFIER_OF_TYPE => {
                modifier = MODIFIER_OF_TYPE;
            }
            MODIFIER_TEXT => {
                modifier = MODIFIER_TEXT;
            }
            MODIFIER_TEXT_ADVANCED => {
                modifier = MODIFIER_TEXT_ADVANCED;
            }
            _ => {
                // Modifier can hold resource type as well when the search parameter type is reference
                if fhirRegistry.isSupportedResource(paramSplitResult[1]) {
                    modifier = paramSplitResult[1];
                } else {
                    // Unknown search parameter modifier
                    string msg = "Unknown search parameter modifier : " + paramSplitResult[1];
                    return createFHIRError(msg, ERROR, PROCESSING_NOT_SUPPORTED, 
                                            diagnostic = msg, errorType = VALIDATION_ERROR, 
                                            httpStatusCode = http:STATUS_BAD_REQUEST);
                }
            }
        }

    }
    return {
        originalName: paramName,
        name: qParamName,
        modifier: modifier,
        values: values
    };
}


isolated function decodePrefixedValue(string value) returns [Prefix, string] {
    if value.length() > 2 {
        string prefixStr = value.substring(0, 2);
        log:printDebug(string `prefixStr: ${prefixStr}`);
        string valueStr = value.substring(2, value.length());
        Prefix prefix;
        match prefixStr {
            "eq" => {
                prefix = eq;
            }
            "ne" => {
                prefix = ne;
            }
            "gt" => {
                prefix = gt;
            }
            "lt" => {
                prefix = lt;
            }
            "ge" => {
                prefix = ge;
            }
            "le" => {
                prefix = le;
            }
            "sa" => {
                prefix = sa;
            }
            "eb" => {
                prefix = eb;
            }
            "ap" => {
                prefix = ap;
            }
            _ => {
                prefix = eq;
                // Non of the prefixes matches, assume no prefixes in the param value
                valueStr = value;
            }
        }
        return [prefix, valueStr];
    }
    // not enough characters to decode. Hence assume no prefixes added to the param value
    return [eq, value];
}

isolated function extractActiveSearchParameterNames (map<SearchParamConfig> paramConfig) returns string[] {
    string[] paramNames = [];
    foreach SearchParamConfig config in paramConfig {
        if config.active {
            paramNames.push(config.name);
        }
    }
    return paramNames;
}
