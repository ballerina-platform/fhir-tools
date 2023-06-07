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
import ballerina/time;

isolated function iso8601toCivil(string dateTime) returns time:Civil|error {
    int dateLen = dateTime.length();
    if dateLen >= 4 {

        time:Civil parsedTime = {
            month: 0,
            hour: 0,
            year: 0,
            day: 0,
            minute: 0,
            utcOffset: {hours: 0}
        };

        int year = check int:fromString(dateTime.substring(0,4));
        parsedTime.year = year;
        if dateLen >= 7 {
            int month = check int:fromString(dateTime.substring(5,7));
            parsedTime.month = month;
            if dateLen >= 10 {
                int day = check int:fromString(dateTime.substring(8,10));
                parsedTime.day = day;
                if dateLen >= 11 {
                    string[] splittedValues = regex:split(dateTime, "T");
                    if splittedValues.length() == 2 {
                        string timePart = splittedValues[1];
                        int timePartLen = timePart.length();
                        if timePartLen >= 5 {
                            int hours = check int:fromString(timePart.substring(0,2));
                            int minutes = check int:fromString(timePart.substring(3,5));
                            parsedTime.hour = hours;
                            parsedTime.minute = minutes;
                            if timePartLen >= 8 {
                                decimal seconds = check decimal:fromString(timePart.substring(6,8));
                                parsedTime.second = seconds;
                                if timePartLen > 8 {
                                    if timePart.endsWith("Z") {
                                        time:ZoneOffset zoneOffset = {
                                            hours: 0,
                                            minutes: 0
                                        };
                                        parsedTime.utcOffset = zoneOffset;
                                    } else {
                                        int offsetSign = 1;
                                        int? indexOfPlus = timePart.indexOf("+");
                                        string timeZone;
                                        if indexOfPlus != () {
                                            timeZone = timePart.substring(indexOfPlus);
                                        } else {
                                            int? indexOfMinus = timePart.indexOf("-");
                                            if indexOfMinus != () {
                                                offsetSign = -1;
                                                timeZone = timePart.substring(indexOfMinus);
                                            } else {
                                                // Return error
                                                string msg = "Error occurred while parsing iso8601 format." +
                                                                " Timezone [+/-] sign is missing." +
                                                                "Expected format:YYYY-MM-DDThh:mm:ssTZD";
                                                return error(msg);
                                            }
                                        }
                                        time:ZoneOffset zoneOffset = {
                                            hours: (offsetSign * check int:fromString(timeZone.substring(1,3))),
                                            minutes: (offsetSign * check int:fromString(timeZone.substring(4,6)))
                                        };
                                        parsedTime.utcOffset = zoneOffset;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return parsedTime;
    } else {
        string msg = "Error occurred while parsing iso8601 format. " +
                        "Expected formats :YYYY, YYYY-MM, YYYY-MM-DD, YYYY-MM-DDThh:mm:ss, YYYY-MM-DDThh:mm:ss.sTZD";
        return error(msg);
    }
}