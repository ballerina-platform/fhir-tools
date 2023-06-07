// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Definition of an element in a resource or extension
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations
# + modifierExtension - Additional content defined by implementations
# + path - Path of the element in the hierarchy of elements  
# + representation - xmlAttr | xmlText | typeAttr | cdaText | xhtml
# + sliceName - Name for this particular element (in a set of slices)
# + sliceIsConstraining - If this slice definition constrains an inherited slice definition (or not)
# + label - Name for element to display with or prompt for element
# + code - Corresponding codes in terminologies
# + slicing - This element is sliced - slices follow
# + short - Concise definition for space-constrained presentation
# + definition - Full formal definition as narrative text
# + comment - Comments about the use of this element
# + requirements - Why this resource has been created
# + alias - Other names
# + min - Minimum Cardinality
# + max - Maximum Cardinality
# + base - Base definition information for tools
# + contendReference - Reference to definition of content for the element
# + 'type - Data type and Profile for this element
# + defaultValue - Specified value if missing from instance
# + meaningWhenMissing - Implicit meaning when this element is missing
# + orderMeaning - What the order of the elements means
# + fixed - Value must be exactly this
# + pattern - Value must have at least these property values
# + example - Example value (as defined for type)
# + minValueDate - Minimum Allowed Value for date
# + minValueDateTime - Minimum Allowed Value for datetime
# + minValueInstant - Minimum Allowed Value for instant
# + minValueTime - Minimum Allowed Value for time
# + minValueDecimal - Minimum Allowed Value for decimal
# + minValueInteger - Minimum Allowed Value for integer
# + minValuePositiveInt - Minimum Allowed Value for positiveint
# + minValueUnsignedInt - Minimum Allowed Value for unsignedint
# + minValueQuantity - Minimum Allowed Value for quantity
# + maxValueDate - Maximum Allowed Value for date
# + maxValueDateTime - Maximum Allowed Value for datetime
# + maxValueInstant - Maximum Allowed Value for instant
# + maxValueTime - Maximum Allowed Value for time
# + maxValueDecimal - Maximum Allowed Value for decimal
# + maxValueInteger - Maximum Allowed Value for integer
# + maxValuePositiveInt - Maximum Allowed Value for positiveint
# + maxValueUnsignedInt - Maximum Allowed Value for unsignedint
# + maxValueQuantity - Maximum Allowed Value for quantity
# + maxLength - Maximum Allowed Value for length
# + condition - Reference to invariant about presence
# + constraint - Condition that must evaluate to true
# + mustSupport - If the element must be supported
# + isModifier - If this modifies the meaning of other elements
# + isModifierReason - Reason that this element is marked as a modifier
# + isSummary - Include when _summary = true?
# + binding - ValueSet details if this is coded
# + mapping - Map element to another set of definitions
@DataTypeDefinition {
    name: "ElementDefinition",
    baseType: BackboneElement,
    elements: {
        "path": {
            name: "path",
            dataType: string,
            min: 1,
            max: 1,
            isArray: false,
            description: "Path of the element in the hierarchy of elements"
        },
        "representation": {
            name: "representation",
            dataType: code,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "xmlAttr | xmlText | typeAttr | cdaText | xhtml",
            valueSet: "https://hl7.org/fhir/valueset-property-representation.html"
        },
        "sliceName": {
            name: "sliceName",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Name for this particular element (in a set of slices)"
        },
        "sliceIsConstraining": {
            name: "sliceIsConstraining",
            dataType: boolean,
            min: 0,
            max: 1,
            isArray: false,
            description: "If this slice definition constrains an inherited slice definition (or not)"
        },
        "label": {
            name: "label",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Name for element to display with or prompt for element"
        },
        "code": {
            name: "code",
            dataType: Coding,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Corresponding codes in terminologies",
            valueSet: "https://hl7.org/fhir/valueset-observation-codes.html"
        },
        "slicing": {
            name: "slicing",
            dataType: ElementSlicing,
            min: 0,
            max: 1,
            isArray: false,
            description: "This element is sliced - slices follow"
        },
        "short": {
            name: "short",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Concise definition for space-constrained presentation"
        },
        "definition": {
            name: "definition",
            dataType: markdown,
            min: 0,
            max: 1,
            isArray: false,
            description: "Full formal definition as narrative text"
        },
        "comment": {
            name: "comment",
            dataType: markdown,
            min: 0,
            max: 1,
            isArray: false,
            description: "Comments about the use of this element"
        },
        "requirements": {
            name: "requirements",
            dataType: markdown,
            min: 0,
            max: 1,
            isArray: false,
            description: "Why this resource has been created"
        },
        "alias": {
            name: "alias",
            dataType: string,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Other names"
        },
        "min": {
            name: "min",
            dataType: unsignedInt,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Cardinality"
        },
        "max": {
            name: "max",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Cardinality"
        },
        "base": {
            name: "base",
            dataType: ElementBase,
            min: 0,
            max: 1,
            isArray: false,
            description: "Base definition information for tools"
        },
        "contendReference": {
            name: "contendReference",
            dataType: uri,
            min: 0,
            max: 1,
            isArray: false,
            description: "Reference to definition of content for the element"
        },
        "type": {
            name: "type",
            dataType: ElementType,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Data type and Profile for this element"
        },
        "defaultValue": {
            name: "defaultValue",
            dataType: Element,
            min: 0,
            max: 1,
            isArray: true,
            description: "Specified value if missing from instance"
        },
        "meaningWhenMissing": {
            name: "meaningWhenMissing",
            dataType: markdown,
            min: 0,
            max: 1,
            isArray: false,
            description: "Implicit meaning when this element is missing"
        },
        "orderMeaning": {
            name: "orderMeaning",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "What the order of the elements means"
        },
        "fixed": {
            name: "fixed",
            dataType: Element,
            min: 0,
            max: 1,
            isArray: false,
            description: "Value must be exactly this"
        },
        "pattern": {
            name: "pattern",
            dataType: Element,
            min: 0,
            max: 1,
            isArray: false,
            description: "Value must have at least these property values"
        },
        "example": {
            name: "example",
            dataType: ElementExample,
            min: 0,
            max: 1,
            isArray: false,
            description: "What the order of the elements means"
        },
        "minValueDate": {
            name: "minValueDate",
            dataType: date,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Allowed Value (for some types)"
        },
        "minValueDateTime": {
            name: "minValueDateTime",
            dataType: dateTime,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Allowed Value (for some types)"
        },
        "minValueInstant": {
            name: "minValueInstant",
            dataType: instant,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Allowed Value (for some types)"
        },
        "minValueTime": {
            name: "minValueTime",
            dataType: time,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Allowed Value (for some types)"
        },
        "minValueDecimal": {
            name: "minValueDecimal",
            dataType: decimal,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Allowed Value (for some types)"
        },
        "minValueInteger": {
            name: "minValueInteger",
            dataType: integer,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Allowed Value (for some types)"
        },
        "minValuePositiveInt": {
            name: "minValuePositiveInt",
            dataType: positiveInt,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Allowed Value (for some types)"
        },
        "minValueUnsignedInt": {
            name: "minValueUnsignedInt",
            dataType: unsignedInt,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Allowed Value (for some types)"
        },
        "minValueQuantity": {
            name: "minValueQuantity",
            dataType: Quantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Minimum Allowed Value (for some types)"
        },
        "maxValueDate": {
            name: "maxValueDate",
            dataType: date,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Allowed Value (for some types)"
        },
        "maxValueDateTime": {
            name: "maxValueDateTime",
            dataType: dateTime,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Allowed Value (for some types)"
        },
        "maxValueInstant": {
            name: "maxValueInstant",
            dataType: instant,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Allowed Value (for some types)"
        },
        "maxValueTime": {
            name: "maxValueTime",
            dataType: time,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Allowed Value (for some types)"
        },
        "maxValueDecimal": {
            name: "maxValueDecimal",
            dataType: decimal,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Allowed Value (for some types)"
        },
        "maxValueInteger": {
            name: "maxValueInteger",
            dataType: integer,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Allowed Value (for some types)"
        },
        "maxValuePositiveInt": {
            name: "maxValuePositiveInt",
            dataType: positiveInt,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Allowed Value (for some types)"
        },
        "maxValueUnsignedInt": {
            name: "maxValueUnsignedInt",
            dataType: unsignedInt,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Allowed Value (for some types)"
        },
        "maxValueQuantity": {
            name: "maxValueQuantity",
            dataType: Quantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Maximum Allowed Value (for some types)"
        },
        "maxLength": {
            name: "maxLength",
            dataType: integer,
            min: 0,
            max: 1,
            isArray: false,
            description: "Max length for strings"
        },
        "condition": {
            name: "condition",
            dataType: id,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Reference to invariant about presence"
        },
        "constraint": {
            name: "constraint",
            dataType: ElementConstraint,
            min: 0,
            max: 1,
            isArray: false,
            description: "Condition that must evaluate to true"
        },
        "mustSupport": {
            name: "mustSupport",
            dataType: boolean,
            min: 0,
            max: 1,
            isArray: false,
            description: "If the element must be supported"
        },
        "isModifier": {
            name: "isModifier",
            dataType: boolean,
            min: 0,
            max: 1,
            isArray: false,
            description: "If this modifies the meaning of other elements"
        },
        "isModifierReason": {
            name: "isModifierReason",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Reason that this element is marked as a modifier"
        },
        "isSummary": {
            name: "isSummary",
            dataType: boolean,
            min: 0,
            max: 1,
            isArray: false,
            description: "Include when _summary = true?"
        },
        "binding": {
            name: "binding",
            dataType: ElementBinding,
            min: 0,
            max: 1,
            isArray: false,
            description: "ValueSet details if this is coded"
        },
        "mapping": {
            name: "mapping",
            dataType: ElementMapping,
            min: 0,
            max: 1,
            isArray: false,
            description: "Map element to another set of definitions"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    },
    validator: elementDefinitionDataTypeValidationFunction
}
public type ElementDefinition record {|
    *BackboneElement;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    Extension[] modifierExtension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    string path;
    code[] representation?;
    string sliceName?;
    boolean sliceIsConstraining?;
    string label?;
    Coding[] code?;
    ElementSlicing slicing?;
    string short?;
    markdown definition?;
    markdown comment?;
    markdown requirements?;
    string[] alias?;
    unsignedInt min?;
    string max?;
    ElementBase base?;
    uri contendReference?;
    ElementType[] 'type?;
    Element defaultValue?;
    markdown meaningWhenMissing?;
    string orderMeaning?;
    Element fixed?;
    Element pattern?;
    ElementExample[] example?;

    date minValueDate?;
    dateTime minValueDateTime?;
    instant minValueInstant?;
    time minValueTime?;
    decimal minValueDecimal?;
    integer minValueInteger?;
    positiveInt minValuePositiveInt?;
    unsignedInt minValueUnsignedInt?;
    Quantity minValueQuantity?;

    date maxValueDate?;
    dateTime maxValueDateTime?;
    instant maxValueInstant?;
    time maxValueTime?;
    decimal maxValueDecimal?;
    integer maxValueInteger?;
    positiveInt maxValuePositiveInt?;
    unsignedInt maxValueUnsignedInt?;
    Quantity maxValueQuantity?;

    integer maxLength?;
    id[] condition?;
    ElementConstraint[] constraint?;
    boolean mustSupport?;
    boolean isModifier?;
    string isModifierReason?;
    boolean isSummary?;
    ElementBinding binding?;
    ElementMapping[] mapping?;

|};

public isolated function elementDefinitionDataTypeValidationFunction(anydata data,
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

        date? minValueDateVal = <date>mapObj.get("minValueDate");
        dateTime? minValueDateTimeVal = <dateTime>mapObj.get("minValueDateTime");
        instant? minValueInstantVal = <instant>mapObj.get("minValueInstant");
        time? minValueTimeVal = <time>mapObj.get("minValueTime");
        decimal? minValueDecimalVal = <decimal>mapObj.get("minValueDecimal");
        integer? minValueIntegerVal = <integer>mapObj.get("minValueInteger");
        positiveInt? minValuePositiveIntVal = <positiveInt>mapObj.get("minValuePositiveInt");
        unsignedInt? minValueUnsignedIntVal = <unsignedInt>mapObj.get("minValueUnsignedInt");
        Quantity? minValueQuantityVal = <Quantity>mapObj.get("minValueQuantity");

        boolean minValueDateValCheck = minValueDateVal is ();
        boolean minValueDateTimeValCheck = minValueDateTimeVal is ();
        boolean minValueInstantValCheck = minValueInstantVal is ();
        boolean minValueTimeValCheck = minValueTimeVal is ();
        boolean minValueDecimalValCheck = minValueDecimalVal is ();
        boolean minValueIntegerValCheck = minValueIntegerVal is ();
        boolean minValuePositiveIntValCheck = minValuePositiveIntVal is ();
        boolean minValueUnsignedIntValCheck = minValueUnsignedIntVal is ();
        boolean minValueQuantityValCheck = minValueQuantityVal is ();

        boolean expression = (minValueDateValCheck) && (minValueDateTimeValCheck) && (minValueInstantValCheck) && 
                        (minValueTimeValCheck) && (minValueDecimalValCheck) && (minValueIntegerValCheck) && (minValuePositiveIntValCheck) && (minValueUnsignedIntValCheck) && (!minValueQuantityValCheck) 
                        || (minValueDateValCheck) && (minValueDateTimeValCheck) && (minValueInstantValCheck) && (minValueTimeValCheck) && (minValueDecimalValCheck) && (minValueIntegerValCheck) && (minValuePositiveIntValCheck) && (!minValueUnsignedIntValCheck) && (minValueQuantityValCheck)
                        || (minValueDateValCheck) && (minValueDateTimeValCheck) && (minValueInstantValCheck) && (minValueTimeValCheck) && (minValueDecimalValCheck) && (minValueIntegerValCheck) && (!minValuePositiveIntValCheck) && (minValueUnsignedIntValCheck) && (minValueQuantityValCheck) 
                        || (minValueDateValCheck) && (minValueDateTimeValCheck) && (minValueInstantValCheck) && (minValueTimeValCheck) && (minValueDecimalValCheck) && (!minValueIntegerValCheck) && (minValuePositiveIntValCheck) && (minValueUnsignedIntValCheck) && (minValueQuantityValCheck)
                        || (minValueDateValCheck) && (minValueDateTimeValCheck) && (minValueInstantValCheck) && (minValueTimeValCheck) && (!minValueDecimalValCheck) && (minValueIntegerValCheck) && (minValuePositiveIntValCheck) && (minValueUnsignedIntValCheck) && (minValueQuantityValCheck) 
                        || (minValueDateValCheck) && (minValueDateTimeValCheck) && (minValueInstantValCheck) && (!minValueTimeValCheck) && (minValueDecimalValCheck) && (minValueIntegerValCheck) && (minValuePositiveIntValCheck) && (minValueUnsignedIntValCheck) && (minValueQuantityValCheck)
                        || (minValueDateValCheck) && (minValueDateTimeValCheck) && (!minValueInstantValCheck) && (minValueTimeValCheck) && (minValueDecimalValCheck) && (minValueIntegerValCheck) && (minValuePositiveIntValCheck) && (minValueUnsignedIntValCheck) && (minValueQuantityValCheck) 
                        || (minValueDateValCheck) && (!minValueDateTimeValCheck) && (minValueInstantValCheck) && (minValueTimeValCheck) && (minValueDecimalValCheck) && (minValueIntegerValCheck) && (minValuePositiveIntValCheck) && (minValueUnsignedIntValCheck) && (minValueQuantityValCheck)
                        || (!minValueDateValCheck) && (minValueDateTimeValCheck) && (minValueInstantValCheck) && (minValueTimeValCheck) && (minValueDecimalValCheck) && (minValueIntegerValCheck) && (minValuePositiveIntValCheck) && (minValueUnsignedIntValCheck) && (minValueQuantityValCheck);

        date? maxValueDateVal = <date>mapObj.get("maxValueDate");
        dateTime? maxValueDateTimeVal = <dateTime>mapObj.get("maxValueDateTime");
        instant? maxValueInstantVal = <instant>mapObj.get("maxValueInstant");
        time? maxValueTimeVal = <time>mapObj.get("maxValueTime");
        decimal? maxValueDecimalVal = <decimal>mapObj.get("maxValueDecimal");
        integer? maxValueIntegerVal = <integer>mapObj.get("maxValueInteger");
        positiveInt? maxValuePositiveIntVal = <positiveInt>mapObj.get("maxValuePositiveInt");
        unsignedInt? maxValueUnsignedIntVal = <unsignedInt>mapObj.get("maxValueUnsignedInt");
        Quantity? maxValueQuantityVal = <Quantity>mapObj.get("maxValueQuantity");

        boolean maxValueDateValCheck = maxValueDateVal is ();
        boolean maxValueDateTimeValCheck = maxValueDateTimeVal is ();
        boolean maxValueInstantValCheck = maxValueInstantVal is ();
        boolean maxValueTimeValCheck = maxValueTimeVal is ();
        boolean maxValueDecimalValCheck = maxValueDecimalVal is ();
        boolean maxValueIntegerValCheck = maxValueIntegerVal is ();
        boolean maxValuePositiveIntValCheck = maxValuePositiveIntVal is ();
        boolean maxValueUnsignedIntValCheck = maxValueUnsignedIntVal is ();
        boolean maxValueQuantityValCheck = maxValueQuantityVal is ();

        boolean expressionTwo = (maxValueDateValCheck) && (maxValueDateTimeValCheck) && (maxValueInstantValCheck) && 
                        (maxValueTimeValCheck) && (maxValueDecimalValCheck) && (maxValueIntegerValCheck) && (maxValuePositiveIntValCheck) && (maxValueUnsignedIntValCheck) && (!maxValueQuantityValCheck) 
                        || (maxValueDateValCheck) && (maxValueDateTimeValCheck) && (maxValueInstantValCheck) && (maxValueTimeValCheck) && (maxValueDecimalValCheck) && (maxValueIntegerValCheck) && (maxValuePositiveIntValCheck) && (!maxValueUnsignedIntValCheck) && (maxValueQuantityValCheck) 
                        || (maxValueDateValCheck) && (maxValueDateTimeValCheck) && (maxValueInstantValCheck) && (maxValueTimeValCheck) && (maxValueDecimalValCheck) && (maxValueIntegerValCheck) && (!maxValuePositiveIntValCheck) && (maxValueUnsignedIntValCheck) && (maxValueQuantityValCheck) 
                        || (maxValueDateValCheck) && (maxValueDateTimeValCheck) && (maxValueInstantValCheck) && (maxValueTimeValCheck) && (maxValueDecimalValCheck) && (!maxValueIntegerValCheck) && (maxValuePositiveIntValCheck) && (maxValueUnsignedIntValCheck) && (maxValueQuantityValCheck) 
                        || (maxValueDateValCheck) && (maxValueDateTimeValCheck) && (maxValueInstantValCheck) && (maxValueTimeValCheck) && (!maxValueDecimalValCheck) && (maxValueIntegerValCheck) && (maxValuePositiveIntValCheck) && (maxValueUnsignedIntValCheck) && (maxValueQuantityValCheck) 
                        || (maxValueDateValCheck) && (maxValueDateTimeValCheck) && (maxValueInstantValCheck) && (!maxValueTimeValCheck) && (maxValueDecimalValCheck) && (maxValueIntegerValCheck) && (maxValuePositiveIntValCheck) && (maxValueUnsignedIntValCheck) && (maxValueQuantityValCheck) 
                        || (maxValueDateValCheck) && (maxValueDateTimeValCheck) && (!maxValueInstantValCheck) && (maxValueTimeValCheck) && (maxValueDecimalValCheck) && (maxValueIntegerValCheck) && (maxValuePositiveIntValCheck) && (maxValueUnsignedIntValCheck) && (maxValueQuantityValCheck) 
                        || (maxValueDateValCheck) && (!maxValueDateTimeValCheck) && (maxValueInstantValCheck) && (maxValueTimeValCheck) && (maxValueDecimalValCheck) && (maxValueIntegerValCheck) && (maxValuePositiveIntValCheck) && (maxValueUnsignedIntValCheck) && (maxValueQuantityValCheck) 
                        || (!maxValueDateValCheck) && (maxValueDateTimeValCheck) && (maxValueInstantValCheck) && (maxValueTimeValCheck) && (maxValueDecimalValCheck) && (maxValueIntegerValCheck) && (maxValuePositiveIntValCheck) && (maxValueUnsignedIntValCheck) && (maxValueQuantityValCheck);

        if (expression) {
            if (expressionTwo) {
                return;
            }
            else {
                string diagnosticMsg = "Error occurred due to incorrect definition of maxValue attribute in " +
                                "ElementDefinition element according to FHIR Specification";
                return <FHIRValidationError>createInternalFHIRError(
                                "Error occurred due to incorrect data type definition", FATAL, PROCESSING,
                                diagnostic = diagnosticMsg);
            }
        }
        else {
            string diagnosticMsg = "Error occurred due to incorrect definition of minValue attribute in " +
                            "ElementDefinition element according to FHIR Specification";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred due to incorrect data type definition", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg);
        }

    }
}
