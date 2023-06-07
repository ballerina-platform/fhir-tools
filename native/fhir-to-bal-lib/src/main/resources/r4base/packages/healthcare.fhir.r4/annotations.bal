// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Function definition for data type serialization
public type DataTypeSerializerFunction isolated function (anydata data, ElementAnnotationDefinition elementContextDefinition)
                                                                            returns (FHIRWireFormat|FHIRSerializerError)?;
# Function definition for FHIR resource serialization
public type ResourceSerializerFunction isolated function (anydata data) returns (FHIRWireFormat|FHIRSerializerError)?;

# Function definition for FHIR container resource serialization
public type ContainerSerializerFunction isolated function (Bundle data) returns (FHIRWireFormat|FHIRSerializerError)?;

# Function definition for FHIR data type validation
public type DataTypeValidationFunction isolated function (anydata data, ElementAnnotationDefinition elementContextDefinition)
                                                                                            returns FHIRValidationError?;
# Function definition for FHIR resource validation
public type ResourceTypeValidationFunction isolated function (anydata data) returns FHIRValidationError?;


# Represents definition of a FHIR resource
#
# + resourceType - FHIR resource type
# + baseType - Type of the resource / abstract resource that is based on
# + profile - profile url
# + elements - element definitions contained in the resource
# + serializers - Serializers of the resource
# + validator - Validator for the resource
# + processingMetaInfo - Meta information for processing the resource (This is included in intermediate user friendly FHIR models)
public type ResourceDefinitionRecord record {
    string resourceType;
    typedesc? baseType;
    string? profile;
    map<ElementAnnotationDefinition> elements;
    ResourceSerializerCollection serializers;
    ResourceTypeValidationFunction validator?;
    ProcessingMetaInfo processingMetaInfo?;
};

# Represents definition of a FHIR resource
#
# + name - Name of the resource
# + baseType - Type of the resource / abstract resource that is based on
# + profile - profile url
# + elements - element definitions contained in the resource
# + serializers - Serializers of the resource
# + validator - Validator for the resource
# + processingMetaInfo - Meta information for processing the resource (This is included in intermediate user friendly FHIR models)
public type ContainerDefinitionRecord record {
    string name;
    typedesc? baseType;
    string? profile;
    map<ElementAnnotationDefinition> elements;
    ContainerSerializerCollection serializers;
    ResourceTypeValidationFunction validator?;
    ProcessingMetaInfo processingMetaInfo?;
};

# Definition if a FHIR complex data type
#
# + name - Name of the data type
# + baseType - Base Data type that this data type is based on (extended from)
# + elements - Child elements of the defined data type
# + serializers - Serializer of the data type
# + validator - Validator for the data type
# + processingMetaInfo - Meta information for processing the data entry (This is included in intermediate user friendly FHIR models)
public type DataTypeDefinitionRecord record {
    string name;
    typedesc? baseType;
    map<ElementAnnotationDefinition> elements;
    DataTypeSerializerCollection serializers;
    DataTypeValidationFunction validator?;
    ProcessingMetaInfo processingMetaInfo?;
};

# Element definition
#
# + name - Name of the element
# + dataType - Data type of the element
# + min - Minimum Cardinality
# + max - Maximum Cardinality (a number or *). In ballerina * is represented by maximum int value
# + isArray - Is this element holds array (is cardinality > 1)
# + description - Description of the elemenet
# + path - FHIR path pf the element respective to the parent
# + valueSet - If coded values are bound to  a ValueSet
# + mustSupport -  The following data-elements are mandatory (i.e data MUST be present) or must be supported if the data is present in the sending system
public type ElementAnnotationDefinition record {
    string name;
    typedesc dataType;
    int min;
    int max;
    boolean isArray;
    string description?;
    string path?;
    string valueSet?;
    boolean mustSupport?;
};

# Resource Serializer Collection
#
# + 'xml - xml serializer
# + 'json - json serializer
public type ResourceSerializerCollection record {
    ResourceSerializerFunction 'xml;
    ResourceSerializerFunction 'json;
};

# Data type serializer collection
#
# + 'xml - xml serializer
# + 'json - json serializer
public type DataTypeSerializerCollection record {
    DataTypeSerializerFunction 'xml;
    DataTypeSerializerFunction 'json;
};

# Resource Serializer Collection
#
# + 'xml - xml serializer
# + 'json - json serializer
public type ContainerSerializerCollection record {
    ContainerSerializerFunction 'xml;
    ContainerSerializerFunction 'json;
};

# Record type to hold meta information used for processing the entity
#
# + targetModel - target resource model or data model  
# + relocations - mappings entries from source model (this) to target model
public type ProcessingMetaInfo record {
    typedesc targetModel?;
    Mapping[] relocations?;
};

# Record type to hold relocation mapping information
#
# + sourcePath - FHIR path to source  
# + targetPath - FHIR path to target location
public type Mapping record {
    string sourcePath;
    string targetPath;
};

public annotation ResourceDefinitionRecord ResourceDefinition on type;
public annotation ContainerDefinitionRecord ContainerDefinition on type;
public annotation DataTypeDefinitionRecord DataTypeDefinition on type;
