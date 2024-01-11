/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.healthcare.fhir.ballerina.prebuiltservicegen.tool;

import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This generates FHIR service interaction method content.
 */
public class ServiceInteractionMethodGenerator extends AbstractFHIRTemplateGenerator {

    private final Map<String, String> interactionMethodContent = new HashMap<>();

    public ServiceInteractionMethodGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        if (generatorProperties.containsKey("interactionType")) {
            String content = this.getTemplateEngine().generateOutputAsString(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                    File.separator + "interaction_method_content.vm", createTemplateContext(generatorProperties));
            if (!Objects.equals(content, "")) {
                interactionMethodContent.put((String) generatorProperties.get("interactionType"), content);
            }
        }
    }

    private TemplateContext createTemplateContext(Map<String, Object> generatorProperties) {
        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty("resourceType", generatorProperties.get("resourceType"));
        templateContext.setProperty("interactionType", generatorProperties.get("interactionType"));
        templateContext.setProperty("profileList", generatorProperties.get("profileList"));
        return templateContext;
    }

    public String getInteractionMethodContent(String interactionType) {
        return interactionMethodContent.get(interactionType);
    }
}
