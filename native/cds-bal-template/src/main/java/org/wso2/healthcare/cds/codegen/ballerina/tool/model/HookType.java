/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.cds.codegen.ballerina.tool.model;

public enum HookType {

    PATIENT_VIEW("patient-view"),
    ORDER_SIGN("order-sign"),
    ORDER_SELECT("order-select"),
    ORDER_DISPATCH("order-dispatch"),
    ENCOUNTER_START("encounter-start"),
    ENCOUNTER_DISCHARGE("encounter-discharge"),
    APPOINTMENT_BOOK("appointment-book");

    private final String value;

    HookType(String s) {
        value = s;
    }

    public String getValue() {
        return value;
    }

    public static HookType fromString(String text) {
        for (HookType h : HookType.values()) {
            if (h.value.equalsIgnoreCase(text)) {
                return h;
            }
        }
        return null;
    }
}
