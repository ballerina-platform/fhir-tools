// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# A time period defined by a start and end date/time. 
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations 
# + 'start - Starting time with inclusive boundary  
# + end - End time with inclusive boundary, if not ongoing
@DataTypeDefinition {
    name: "Period",
    baseType: Element,
    elements: {
        "start" : {
            name: "start",
            dataType: dateTime,
            min: 0,
            max: 1,
            isArray: false,
            description: "Starting time with inclusive boundary"
        },
        "end" : {
            name: "end",
            dataType: dateTime,
            min: 0,
            max: 1,
            isArray: false,
            description: "End time with inclusive boundary, if not ongoing"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer, 
        'json: complexDataTypeJsonSerializer
    }
}
public type Period record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    dateTime 'start?;
    dateTime end?;
|};
