/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool;

import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.versions.r4.R4BallerinaProjectTool;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.versions.r5.R5BallerinaProjectTool;

public class BallerinaProjectToolFactory {
    public AbstractBallerinaProjectTool getBallerinaProjectTool(String fhirVersion) {
        if (fhirVersion.equals("r4")) {
            return new R4BallerinaProjectTool();
        } else if (fhirVersion.equals("r5")) {
            return new R5BallerinaProjectTool();
        } else {
            throw new IllegalArgumentException("Unsupported FHIR version: " + fhirVersion);
        }
    }
}
