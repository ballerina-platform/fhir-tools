// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Set of values bounded by low and high
# Rule: If present, low SHALL have a lower value than high Elements defined in Ancestors: id, extension
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + low - Low limit 
# + high - High limit
@DataTypeDefinition {
    name: "Range",
    baseType: Element,
    elements: {
        "low": {
            name: "low",
            dataType: SimpleQuantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "Low limit"
        },
        "high": {
            name: "high",
            dataType: SimpleQuantity,
            min: 0,
            max: 1,
            isArray: false,
            description: "High limit"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    },
    validator: rangeDataTypeValidationFunction
}
public type Range record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    SimpleQuantity low?;
    SimpleQuantity high?;
|};

public isolated function rangeDataTypeValidationFunction(anydata data,
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
        SimpleQuantity? lowVal = <SimpleQuantity?>mapObj.get("low");
        SimpleQuantity? highVal = <SimpleQuantity?>mapObj.get("high");

        boolean lowValCheck = lowVal is ();
        boolean highValCheck = highVal is ();
        boolean expression = (lowValCheck) && (highValCheck) || (!lowValCheck) && (!highValCheck);
        if (expression) {
            return;
        }
        else {
            string diagnosticMsg = "Error occurred due to incorrect definition of low or high attribute in Range element " +
                            "according to FHIR Specification";
            return <FHIRValidationError>createInternalFHIRError(
                            "Error occurred due to incorrect data type definition", FATAL, PROCESSING,
                            diagnostic = diagnosticMsg);
        }

    }
}
