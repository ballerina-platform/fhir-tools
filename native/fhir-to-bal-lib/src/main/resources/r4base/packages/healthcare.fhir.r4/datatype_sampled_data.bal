// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A series of measurements taken by a device Elements defined in Ancestors: id, extension
#
# + id - Unique id for inter-element referencing  
# + extension - Additional content defined by implementations  
# + origin - Zero value and units  
# + period - Number of milliseconds between samples 
# + factor - Multiply data by this before adding to origin 
# + lowerLimit - Lower limit of detection  
# + upperLimit - Upper limit of detection  
# + dimensions - Number of sample points at each time point  
# + data - Decimal values with spaces, or E | U | L
@DataTypeDefinition {
    name: "SampledData",
    baseType: Element,
    elements: {
        "origin": {
            name: "origin",
            dataType: SimpleQuantity,
            min: 1,
            max: 1,
            isArray: false,
            description: "Zero value and units"
        },
        "period": {
            name: "period",
            dataType: decimal,
            min: 1,
            max: 1,
            isArray: false,
            description: "Number of milliseconds between samples"
        },
        "factor": {
            name: "factor",
            dataType: decimal,
            min: 0,
            max: 1,
            isArray: false,
            description: "Multiply data by this before adding to origin"
        },
        "lowerLimit": {
            name: "lowerLimit",
            dataType: decimal,
            min: 0,
            max: 1,
            isArray: false,
            description: "Lower limit of detection"
        },
        "upperLimit": {
            name: "upperLimit",
            dataType: decimal,
            min: 0,
            max: 1,
            isArray: false,
            description: "Upper limit of detection"
        },
        "dimensions": {
            name: "dimensions",
            dataType: positiveInt,
            min: 1,
            max: 1,
            isArray: false,
            description: "Number of sample points at each time point"
        },
        "data": {
            name: "data",
            dataType: string,
            min: 0,
            max: 1,
            isArray: false,
            description: "Decimal values with spaces, or E | U | L"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}

public type SampledData record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    SimpleQuantity origin;
    decimal period;
    decimal factor?;
    decimal lowerLimit?;
    decimal upperLimit?;
    positiveInt dimensions;
    string data?;
|};
