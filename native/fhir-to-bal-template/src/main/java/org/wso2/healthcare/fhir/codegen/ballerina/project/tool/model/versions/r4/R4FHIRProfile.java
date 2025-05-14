/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.versions.r4;

import org.hl7.fhir.r4.model.StructureDefinition;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.FHIRProfile;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.util.BallerinaProjectUtil;

import java.util.*;

public class R4FHIRProfile extends FHIRProfile<StructureDefinition> {
    private StructureDefinition profileDef;

    public R4FHIRProfile(StructureDefinition profileDef, String url, String igName, String resourceType) {
        super(profileDef, url, igName, resourceType);
        this.profileDef = profileDef;
        setName(BallerinaProjectUtil.resolveSpecialCharacters(profileDef.getName()));
    }

    public StructureDefinition getProfileDef() {
        return profileDef;
    }

    public void setProfileDef(StructureDefinition profileDef) {
        this.profileDef = profileDef;
    }
}
