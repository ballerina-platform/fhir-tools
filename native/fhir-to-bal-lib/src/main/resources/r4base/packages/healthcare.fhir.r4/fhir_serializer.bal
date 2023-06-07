// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

import ballerina/regex;
import ballerina/log;

// internal record types
type TargetAction record {
    string targetPath;
    json obj;
};

type RecordMapPair record {
    anydata recordModel;
    json jsonModel;
};

public isolated function complexDataTypeXMLSerializer (anydata data, 
                    ElementAnnotationDefinition elementContextDefinition) returns (FHIRWireFormat|FHIRSerializerError)? {
    DataTypeDefinitionRecord? dataTypeDefinition = (typeof data).@DataTypeDefinition;
    if dataTypeDefinition != () {
        xml:Element resultElement = xml:createElement(elementContextDefinition.name, {});
        xml childElements = resultElement.getChildren();
        map<anydata> mapObj = {};
        do {
	        mapObj = check data.ensureType();
        } on fail error e {
        	string diagnosticMsg = "Error occurred while casting data of type: "+
                                            (typeof data).toBalString() +" to map representation";
            return <FHIRSerializerError>createInternalFHIRError(
                        "Error occurred while casting data to map representation",FATAL, PROCESSING, 
                            diagnostic = diagnosticMsg, cause = e);
        }
        string[] keys = mapObj.keys();
        map<ElementAnnotationDefinition> childElementDefMap = dataTypeDefinition.elements;
        map<ElementAnnotationDefinition> processedElements = {};
        foreach string key in keys {
            ElementAnnotationDefinition? childElementDef;
            if childElementDefMap.hasKey(key) {
                childElementDef = childElementDefMap.get(key);
            } else {
                // Check in parent definitions
                childElementDef = findInheritedPropertyDefinition(dataTypeDefinition, key);
            }

            if childElementDef != () {
                processedElements[key] = childElementDef;
                if childElementDef.isArray {
                    anydata[] childDataArray = <anydata[]> mapObj.get(key);
                    foreach anydata childData in childDataArray {
                        DataTypeDefinitionRecord? dataTypeDefRecord = (typeof  childData).@DataTypeDefinition;
                        if dataTypeDefRecord != () {
                            // Complex FHIR type
                            DataTypeSerializerFunction xmlSerializer = dataTypeDefRecord.serializers.'xml;
                            FHIRWireFormat childXmlEntry = check xmlSerializer(childData, childElementDef);
                            if childXmlEntry is xml {
                                childElements += childXmlEntry;
                            }
                        } else {
                            // Primitive FHIR type
                            childElements += xml:createElement(childElementDef.name, {"value" : childData.toString()});
                        }
                    }
                } else {
                    anydata childData = mapObj.get(key);
                    DataTypeDefinitionRecord? dataTypeDefRecord = (typeof  childData).@DataTypeDefinition;
                    if dataTypeDefRecord != () {
                        // FHIR defined complex data type
                        DataTypeSerializerFunction xmlSerializer = dataTypeDefRecord.serializers.'xml;
                        FHIRWireFormat childXmlEntry = check xmlSerializer(childData, childElementDef);
                        if childXmlEntry is xml {
                            childElements += childXmlEntry;
                        }
                    } else {
                        // primitive data types
                        // Handle id parameter
                        if key == "id" {
                            map<string> attributes = resultElement.getAttributes();
                            attributes["id"] = childData.toString();
                            continue;
                        }
                        // Primitive FHIR type
                        childElements += xml:createElement(childElementDef.name, {"value" : childData.toString()});
                    }
                }
            }
        }
        resultElement.setChildren(childElements);
        return resultElement;
    }
    return;
}

public isolated function complexDataTypeJsonSerializer (anydata data, ElementAnnotationDefinition elementContextDefinition) returns (FHIRWireFormat|FHIRSerializerError)? {
    return data.toJson();
}

public isolated function fhirResourceXMLSerializer(anydata data) returns (FHIRWireFormat|FHIRSerializerError)? {
    anydata transformedResource = {};
    do {
	    transformedResource = check doInternalResourceRelocation(data);
    } on fail error e {
    	string diagnosticMsg = "Error occurred while record relocation";
        return <FHIRSerializerError>createInternalFHIRError(diagnosticMsg,FATAL, PROCESSING, 
                                                                    diagnostic = diagnosticMsg, cause = e);
    }
    ResourceDefinitionRecord? resourceDefinition = (typeof transformedResource).@ResourceDefinition;
    if resourceDefinition != () {
        
        xml:Element resultElement = xml:createElement(resourceDefinition.resourceType, {"xmlns":FHIR_NAMESPACE});
        xml childElements = resultElement.getChildren();

        map<anydata>|error mapObj = transformedResource.ensureType();
        if mapObj is error {
            //TODO Handle this situation
            string diagnosticMsg = "Unable to transform resource data of type : " + (typeof data).toBalString() + " into a map";
            return <FHIRSerializerError>createInternalFHIRError("Failed to transform data into map", 
                                                                FATAL, PROCESSING, diagnostic = diagnosticMsg);
        }
        
        string[] keys = mapObj.keys();
        map<ElementAnnotationDefinition> elementDefinitions = resourceDefinition.elements;

        foreach string key in keys {
            ElementAnnotationDefinition? elementDef;
            if elementDefinitions.hasKey(key) {
                elementDef = elementDefinitions.get(key);
            } else {
                // Check in parent definitions
                elementDef = findInheritedPropertyDefinition(resourceDefinition, key);
            }
            if elementDef != () {
                if elementDef.isArray {
                    anydata[] childDataArray = <anydata[]> mapObj.get(key);
                    foreach anydata childData in childDataArray {
                        DataTypeDefinitionRecord? dataTypeDefRecord = (typeof  childData).@DataTypeDefinition;
                        if dataTypeDefRecord != () {
                            // Complex FHIR type
                            DataTypeSerializerFunction xmlSerializer = dataTypeDefRecord.serializers.'xml;
                            FHIRWireFormat childXmlEntry = check xmlSerializer(childData, elementDef);
                            if childXmlEntry is xml {
                                childElements += childXmlEntry;
                            }
                        } else {
                            // Primitive FHIR type
                            childElements += xml:createElement(elementDef.name, {"value" : childData.toString()});
                        }
                    }
                } else {
                    anydata childData = mapObj.get(key);
                    DataTypeDefinitionRecord? dataTypeDefRecord = (typeof  childData).@DataTypeDefinition;
                    if dataTypeDefRecord != () {
                        // FHIR defined complex data type
                        DataTypeSerializerFunction xmlSerializer = dataTypeDefRecord.serializers.'xml;
                        FHIRWireFormat childXmlEntry = check xmlSerializer(childData, elementDef);
                        if childXmlEntry is xml {
                            childElements += childXmlEntry;
                        }
                    } else {
                        // Primitive FHIR type
                        childElements += xml:createElement(elementDef.name, {"value" : childData.toString()});
                    }
                }
            } else {
                // Unable to find definition of the element
                string diagnosticMsg = "Unknown element with name : \\\"" + key +
                                            "\\\" found under resource: " + resourceDefinition.resourceType;
                return <FHIRSerializerError>createInternalFHIRError(diagnosticMsg,FATAL, PROCESSING, 
                                                                            diagnostic = diagnosticMsg);
            }
        }
        resultElement.setChildren(childElements);
        return resultElement;

    } else {
        string diagnosticMsg = "Provided data does not represent a FHIR resource";
        return <FHIRSerializerError>createInternalFHIRError(diagnosticMsg,FATAL, PROCESSING, 
                                                                            diagnostic = diagnosticMsg);
    }
}

public isolated function fhirResourceJsonSerializer(anydata data) returns (FHIRWireFormat|FHIRSerializerError)? {
    do {
        anydata transformedResource = check doInternalResourceRelocation(data);
        return transformedResource.toJson();
    } on fail error e {
        string diagnosticMsg = "Error occurred while record relocation";
        return <FHIRSerializerError>createInternalFHIRError(diagnosticMsg,FATAL, PROCESSING, 
                                                                    diagnostic = diagnosticMsg, cause = e);
    }
}



isolated function findInheritedPropertyDefinition(ResourceDefinitionRecord|DataTypeDefinitionRecord definition, string propertyName) returns ElementAnnotationDefinition? {
    typedesc? baseType = definition.baseType;
    if baseType != () {
        (ResourceDefinitionRecord|DataTypeDefinitionRecord)? baseTypeDef = baseType.@DataTypeDefinition;
        if baseTypeDef is () {
            baseTypeDef = baseType.@ResourceDefinition;
        }

        if baseTypeDef != () {
            if baseTypeDef.elements.hasKey(propertyName) {
                return baseTypeDef.elements.get(propertyName);
            } else {
                return findInheritedPropertyDefinition(baseTypeDef, propertyName);
            }
        }
    }
    return ();
    
}

public isolated function executeResourceXMLSerializer(anydata fhirResource) returns (xml|FHIRSerializerError) {

    ResourceDefinitionRecord? resourceDefinition = (typeof fhirResource).@ResourceDefinition;
    if resourceDefinition is ResourceDefinitionRecord {
        ResourceSerializerFunction xmlResourceSerializer = resourceDefinition.serializers.'xml;
        FHIRWireFormat xmlWireFormat = check xmlResourceSerializer(fhirResource);
        if xmlWireFormat is xml {
            return xmlWireFormat;
        } else {
            string diagnosticMsg = "XML Serializer did not return a XML result. It returned: " +
                                                                                (typeof xmlWireFormat).toBalString();
            return <FHIRSerializerError>createInternalFHIRError("Resource Serialization failed",FATAL, PROCESSING, 
                                                                    diagnostic = diagnosticMsg);
        }
    } else {
        string diagnosticMsg = "Resource definition not found of the record : " + (typeof fhirResource).toBalString();
        return <FHIRSerializerError>createInternalFHIRError("Resource definition not found",FATAL, PROCESSING, 
                                                                    diagnostic = diagnosticMsg);
    }

}

public isolated function executeResourceJsonSerializer(anydata fhirResource) returns (json|FHIRSerializerError) {
    
    ResourceDefinitionRecord? resourceDefinition = (typeof fhirResource).@ResourceDefinition;
    if resourceDefinition is ResourceDefinitionRecord {
        ResourceSerializerFunction jsonResourceSerializer = resourceDefinition.serializers.'json;
        FHIRWireFormat wireFormat = check jsonResourceSerializer(fhirResource);
        if wireFormat is json {
            return wireFormat;
        } else {
            string diagnosticMsg = "JSON Serializer did not return a JSON result. It returned: " +
                                                                                (typeof wireFormat).toBalString();
            return <FHIRSerializerError>createInternalFHIRError("Resource Serialization failed",FATAL, PROCESSING, 
                                                                    diagnostic = diagnosticMsg);
        }
    } else {
        string diagnosticMsg = "Resource definition not found of the record : " + (typeof fhirResource).toBalString();
        return <FHIRSerializerError>createInternalFHIRError("Resource definition not found",FATAL, PROCESSING, 
                                                                    diagnostic = diagnosticMsg);
    }
}


isolated function doInternalResourceRelocation(anydata dataModel) returns anydata|FHIRProcessingError {

    ProcessingMetaInfo? processingMetaInfo;
    ResourceDefinitionRecord? resourceDefinition = (typeof dataModel).@ResourceDefinition;
    if resourceDefinition is () {
        DataTypeDefinitionRecord? dataTypeDefinition = (typeof dataModel).@DataTypeDefinition;
        if dataTypeDefinition is () {
            string diagMessage = "Resource or Data type definition not found. Data model type found : " + 
                                    (typeof  dataModel).toBalString();
            return <FHIRProcessingError>createInternalFHIRError("Resource or Data type definition not found", ERROR, 
                            PROCESSING_NOT_FOUND, diagnostic = diagMessage);
        }
        processingMetaInfo = dataTypeDefinition.processingMetaInfo;
    } else {
        processingMetaInfo = resourceDefinition.processingMetaInfo;
    }
    
    if processingMetaInfo is ProcessingMetaInfo {

        typedesc? targetModel = processingMetaInfo.targetModel;
        Mapping[]? mappings = processingMetaInfo.relocations;

        if targetModel != () && mappings != () {

            ResourceDefinitionRecord? targetResourceDef = targetModel.@ResourceDefinition;
            if targetResourceDef is () {
                string diagMessage = "Resource definition of target model not found. Data model type found : " + 
                                    (typeof  dataModel).toBalString();
                return <FHIRProcessingError>createInternalFHIRError("Resource definition of target model not found", ERROR, 
                            PROCESSING_NOT_FOUND, diagnostic = diagMessage);
            }

            map<json> dataMap = <map<json>> dataModel.toJson();
            TargetAction[] targetActions = [];
            // Remove entries that need relocation
            foreach Mapping mapping in mappings {
                log:printDebug(string `Relocating: ${mapping.toString()}`);
                RecordMapPair? removedEntry = detachEntry({jsonModel:dataMap, recordModel:dataModel}, mapping.sourcePath);
                if removedEntry != () {
                    log:printDebug(string `Entry detached : ${removedEntry.jsonModel.toString()}`);
                    TargetAction targetAction;
                    
                    if removedEntry.jsonModel is json[] {
                        // if the detached entry is an json array
                        json[] jModel = <json[]>removedEntry.jsonModel;
                        anydata[] recordModel = <anydata[]>removedEntry.recordModel;
                        json[] relocatedJsonArray = [];
                        foreach int i in 0...(jModel.length() - 1) {
                            if jModel[i] is boolean|int|float|decimal|string {
                                relocatedJsonArray.push(jModel[i]);
                            } else {
                                relocatedJsonArray.push(check doRelocateChildEntry({recordModel: recordModel[i], jsonModel: jModel[i]}));
                            }
                        }
                        targetAction = {
                            obj: relocatedJsonArray,
                            targetPath: mapping.targetPath
                        };
                    } else {
                        if removedEntry.jsonModel is boolean|int|float|decimal|string {
                            targetAction = {
                                obj: removedEntry.jsonModel,
                                targetPath: mapping.targetPath
                            };
                        } else {
                            json relocatedJsonModel = check doRelocateChildEntry(removedEntry);
                            targetAction = {
                                obj: relocatedJsonModel,
                                targetPath: mapping.targetPath
                            };
                        }
                    }
                    targetActions.push(targetAction);
                } 
            }
            
            // perform relocation
            foreach TargetAction targetAction in targetActions {
                check relocateEntry(dataMap, targetAction, targetResourceDef);
            }

            // type cast
            do {
	            anydata castedRecord = check dataMap.cloneWithType(<typedesc<anydata>>targetModel);
                return castedRecord;
            } on fail var e {
            	string diagMessage = "Error occured while casting relocated data model : " + dataMap.toBalString() + 
                                                    "to type: " + targetModel.toBalString();
                return <FHIRProcessingError>createInternalFHIRError("Error occured while casting relocated data model.", ERROR, 
                            PROCESSING_NOT_FOUND, diagnostic = diagMessage, cause = e);
            }
            

        } else {
            // internal model mapping processing information not available, so nothing to process
            return dataModel;
        }
    } else {
        // nothing to process
        return dataModel;
    }
    
}

isolated function doRelocateChildEntry(RecordMapPair childPair) returns json|FHIRProcessingError {

    DataTypeDefinitionRecord? dataTypeDefinition = (typeof childPair.recordModel).@DataTypeDefinition;
    if dataTypeDefinition is () {
        string diagMessage = "Unable to find Data type definition of : " + childPair.recordModel.toString();
        return <FHIRProcessingError>createInternalFHIRError("Unable to find Data type definition", ERROR, 
                            PROCESSING, diagnostic = diagMessage);
    }

    ProcessingMetaInfo? processingMetaInfo = dataTypeDefinition.processingMetaInfo;
    if processingMetaInfo is ProcessingMetaInfo {

        Mapping[]? relocations = processingMetaInfo.relocations;
        typedesc? targetModel = processingMetaInfo.targetModel;
        if relocations != () && targetModel != () {

            DataTypeDefinitionRecord? targetTypeDef = targetModel.@DataTypeDefinition;
            if targetTypeDef is () {
                string diagMessage = "Data type definition of target model: " + targetModel.toBalString() + " not found";
                return <FHIRProcessingError>createInternalFHIRError("Data type definition of target model not found", ERROR, 
                            PROCESSING_NOT_FOUND, diagnostic = diagMessage);
            }

            map<json> dataMap = <map<json>>childPair.jsonModel;
            foreach Mapping relocation in relocations {

                log:printDebug(string `Relocating: ${relocation.toString()}`);
                RecordMapPair? removedEntry = detachEntry({jsonModel:dataMap, recordModel:childPair.recordModel}, relocation.sourcePath);
                if removedEntry != () {
                    if removedEntry.jsonModel is json[] {
                        // handle if the detached entry is an array
                        json[] jModel = <json[]>removedEntry.jsonModel;
                        anydata[] recordModel = <anydata[]>removedEntry.recordModel;
                        json[] relocatedJsonArray = [];
                        foreach int i in 0..< jModel.length() {
                            if jModel[i] is boolean|int|float|decimal|string {
                                relocatedJsonArray.push(jModel[i]);
                            } else {
                                // Relocate complex type child entry 
                                relocatedJsonArray.push(check doRelocateChildEntry({recordModel: recordModel[i], jsonModel: jModel[i]}));
                            }
                        }
                        check relocateEntry(dataMap, {obj: relocatedJsonArray, targetPath: relocation.targetPath}, targetTypeDef);

                    } else {
                        json relocatedJson;
                        if !(removedEntry.jsonModel is boolean|int|float|decimal|string) {
                            // Relocate complex type child entry 
                            relocatedJson = check doRelocateChildEntry(removedEntry);
                        } else {
                            relocatedJson = removedEntry.jsonModel;
                        }
                        // relocate the entry
                        check relocateEntry(dataMap, {obj: relocatedJson, targetPath: relocation.targetPath}, targetTypeDef);
                    }
                }
            }
            return dataMap;
        } else {
            // internal model mapping processing information not available, so nothing to process
            return childPair.jsonModel;
        }
    } else {
        // nothing to process
        return childPair.jsonModel;
    }

}



isolated function detachEntry(RecordMapPair recordMapPair, string path) returns RecordMapPair? {
    log:printDebug(string `Detaching entry : ${path}`);
    string[] pathParts = regex:split(path, "\\.");
    map<json> tempMap = <map<json>>recordMapPair.jsonModel;
    map<anydata> tempRecord = <map<anydata>>recordMapPair.recordModel;
    int end = pathParts.length() - 1;

    foreach int i in 0...end {
        string pathPart = pathParts[i];
        if tempMap.hasKey(pathPart) {
            // TODO : handle arrays as well
            json entry = tempMap.get(pathPart);
            anydata recordEntry = tempRecord[pathPart];
            if i == end {
                RecordMapPair pair = {
                    recordModel: recordEntry,
                    jsonModel: tempMap.remove(pathPart)
                };
                return pair;
            } else if entry is map<json> {
                tempMap = entry;
                tempRecord = <map<anydata>>recordEntry;
            } else {
                // target or intermediate entry does not exists
                return ();
            }
        } else {
            // entry not found
            return ();
        }
    }
    return ();
}

// Relocate give given target object in mapObj model map
isolated function relocateEntry(map<json> mapObj, TargetAction targetAction, 
                            ResourceDefinitionRecord|DataTypeDefinitionRecord targetResourceDef) returns FHIRProcessingError? {
                                
    string[] pathParts = regex:split(targetAction.targetPath, "\\.");
    map<json> tempMapObj = mapObj;
    int end = pathParts.length() - 1;

    foreach int i in 0...end {
        string currentTarget = pathParts[i];
        if tempMapObj.hasKey(currentTarget) {
            json entry = tempMapObj.get(currentTarget);
            if i != end {
                // traverse next level
                tempMapObj = <map<json>>entry;
            } else {
                // now at the parent object
                if entry is json[] {
                    // parent is an array
                    json[] entryArray = entry;
                    if targetAction.obj is json[] {
                        // if the object to place is an array, we have to add each
                        foreach json jobj in <json[]>targetAction.obj {
                            entryArray.push(jobj);
                        }
                    } else {
                        entryArray.push(targetAction.obj);
                    }
                } else {
                    tempMapObj[currentTarget] = targetAction.obj;
                }
            }

        } else {
            ElementAnnotationDefinition? elementDef = findInheritedPropertyDefinition(targetResourceDef, currentTarget);
            if elementDef is () {
                // Definition not found
                string diagMessage = "Unknown target element to relocate : " + currentTarget +
                                                    " in target path : " + targetAction.targetPath;
                return <FHIRProcessingError>createInternalFHIRError("Unknown target element found to relocate", ERROR, 
                            PROCESSING_NOT_FOUND, diagnostic = diagMessage);
            } 
            if i != end {
                // entry with path not available, hence create new empty one
                if elementDef.isArray {
                    tempMapObj[currentTarget] = [];
                } else {
                    tempMapObj[currentTarget] = {};
                }
            } else {
                if elementDef.isArray {
                    json[] newArray;
                    if targetAction.obj is json[] {
                        // if the object to place is an array
                        newArray = <json[]>targetAction.obj;
                    } else {
                        newArray = [targetAction.obj];
                    }
                    tempMapObj[currentTarget] = newArray;
                } else {
                    tempMapObj[currentTarget] = targetAction.obj;
                }
            }
        }
    }
}
