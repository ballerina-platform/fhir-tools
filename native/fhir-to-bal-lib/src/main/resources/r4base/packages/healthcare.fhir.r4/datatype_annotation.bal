// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Text node with attribution Elements defined in Ancestors: id, extension
#
# + authorReference - Individual responsible for the annotation  
# + authorString - Individual responsible for the annotation
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + time - When the annotation was made  
# + text - The annotation - text content (as markdown)

@DataTypeDefinition {
    name: "Annotation",
    baseType: Element,
    elements: {
        "authorReference": {
            name: "authorReference",
            dataType: Reference,
            min: 0,
            max: 1,
            isArray: false,
            description: "Individual responsible for the annotation (Practitioner | Patient | RelatedPerson | Organization)"
        },
        "authorString": {
            name: "authorString",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Individual responsible for the annotation"
        },
        "time": {
            name: "time",
            dataType: dateTime,
            min: 0,
            max: 1,
            isArray: false,
            description: "When the annotation was made"
        },
        "text": {
            name: "text",
            dataType: markdown,
            min: 1,
            max: 1,
            isArray: false,
            description: "The annotation - text content (as markdown)"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    },
    validator: annotationDataTypeValidationFunction
}
public type Annotation record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    Reference authorReference?;
    string authorString?;
    dateTime time?;
    markdown text;
|};

public isolated function annotationDataTypeValidationFunction(anydata data,
                    ElementAnnotationDefinition elementContextDefinition) returns FHIRValidationError? {
    DataTypeDefinitionRecord? dataTypeDefinition = (typeof data).@DataTypeDefinition;
    if dataTypeDefinition != () {
        map<anydata> mapObj = {};
        do {
            mapObj = check data.ensureType();
        } on fail error e {
            string diagnosticMsg = "Error occurred while casting data of type: " +
                                            (typeof data).toBalString() + " to map representation";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred while casting data to map representation", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg, cause = e);

        }

        Reference? authorReferenceVal = <Reference?>mapObj.get("authorReference");
        string? authorStringVal = <string?>mapObj.get("authorString");

        boolean authorReferenceValueCheck = authorReferenceVal is ();
        boolean authorStringValCheck = authorStringVal is ();
        boolean expression = authorReferenceValueCheck && (!authorStringValCheck) || 
                        (!authorReferenceValueCheck) && authorStringValCheck;

        if (expression) {
            return;
        }
        else {
            string diagnosticMsg = "Error occurred due to incorrect definition of author element according to FHIR Specification";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred due to incorrect data type definition", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg);
        }
    }
}
