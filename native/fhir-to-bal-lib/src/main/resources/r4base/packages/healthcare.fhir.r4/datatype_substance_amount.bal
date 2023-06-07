// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Chemical substances are a single substance type whose primary defining element is the molecular structure. Chemical substances shall be defined on
# the basis of their complete covalent molecular structure; the presence of a salt (counter-ion) and/or solvates (water, alcohols) is also captured.
# Purity, grade, physical form or particle size are not taken into account in the definition of a chemical substance or in the assignment of a Substance ID
#
# + id - Unique id for inter-element referencing  
# + extension - Additional content defined by implementations
# + modifierExtension - Extensions that cannot be ignored even if unrecognized
# + amountQuantity - Amount as a Quantity  
# + amountRange - Field Amount as a Range  
# + amountString - Amount as a String  
# + amountType - Most elements that require a quantitative value will also have a field called amount type. Amount type should always be specified because  
# + referenceRange - Reference range of possible or expected values

@DataTypeDefinition {
    name: "SubstanceAmount",
    baseType: BackboneElement,
    elements: {
        "amountQuantity": {
            name: "amountQuantity",
            dataType: Quantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Amount as a Quantity"
        },
        "amountRange": {
            name: "amountRange",
            dataType: Range,
            min: 0,
            max: 1,
            isArray: false,
            description: "Amount as a Range"
        },
        "amountString": {
            name: "amountString",
            dataType: Quantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Amount as a single value in String"
        },
        "amountType": {
            name: "amountType",
            dataType: CodeableConcept,
            min: 0,
            max: 1,
            isArray: false,
            description: "Most elements that require a quantitative value will also have a field called amount type. Amount type should always be specified because the actual value of the amount is often dependent on it"
        },
        "amountText": {
            name: "amountText",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "A textual comment on a numeric value"
        },
        "referenceRange": {
            name: "referenceRange",
            dataType: ReferenceRange,
            min: 0,
            max: 1,
            isArray: false,
            description: "Reference range of possible or expected values"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    },
    validator: substanceAmountDataTypeValidationFunction
}
public type SubstanceAmount record {|
    *BackboneElement;
    //Inherited child element from "BackboneElement" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    Extension[] modifierExtension?;
    //Inherited child element from "BackboneElement" (Redefining to maintain order when serialize) (END)

    Quantity amountQuantity?;
    Range amountRange?;
    string amountString?;
    CodeableConcept amountType?;
    ReferenceRange referenceRange?;
|};

# Reference range of possible or expected values
#
# + id - Unique id for inter-element referencing  
# + extension - Additional content defined by implementations
# + lowLimit - Lower limit possible or expected  
# + highLimit - Upper limit possible or expected
@DataTypeDefinition {
    name: "ReferenceRange",
    baseType: Element,
    elements: {
        "lowLimit": {
            name: "lowLimit",
            dataType: Quantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Lower limit possible or expected"
        },
        "highLimit": {
            name: "highLimit",
            dataType: Quantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Upper limit possible or expected"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ReferenceRange record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    Quantity lowLimit?;
    Quantity highLimit?;
|};


public isolated function substanceAmountDataTypeValidationFunction(anydata data,
                    ElementAnnotationDefinition elementContextDefinition) returns (FHIRValidationError)? {
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
        Quantity? amountQuantityVal = <Quantity?>mapObj.get("amountQuantity");
        Range? amountRangeVal = <Range?>mapObj.get("amountRange");
        string? amountStringVal = <string?>mapObj.get("amountString");

        boolean amountQuantityValCheck = amountQuantityVal is ();
        boolean amountRangeValCheck = amountRangeVal is ();
        boolean amountStringValCheck = amountStringVal is ();

        boolean expression = (amountQuantityValCheck) && (amountRangeValCheck) && (!amountStringValCheck)
                        || (amountQuantityValCheck) && (!amountRangeValCheck) && (amountStringValCheck)
                        || (!amountQuantityValCheck) && (amountRangeValCheck) && (amountStringValCheck);

        if (expression) {
            return;
        }
        else {
            string diagnosticMsg = "Error occurred due to incorrect definition of timing attribute " +
                            "of TriggerDefinition element according to FHIR Specificatio";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred due to incorrect data type definition", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg);
        }
    }
}