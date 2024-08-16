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

package org.wso2.healthcare.cds.codegen.ballerina.tool.generator;

import org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants;
import org.wso2.healthcare.cds.codegen.ballerina.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;

import java.util.Map;

import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.*;

public class MetaGenerator extends AbstractFHIRTemplateGenerator {

    public MetaGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = getTargetDir();
        this.getTemplateEngine().generateOutputAsFile(CdsBallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                        CdsBallerinaProjectConstants.RESOURCE_PATH_SEPERATOR + BAL_PACKAGE_MD_VM, createTemplateContextForMeta(generatorProperties), directoryPath,
                BAL_PACKAGE_MD_FILE);
        this.getTemplateEngine().generateOutputAsFile(CdsBallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                        CdsBallerinaProjectConstants.RESOURCE_PATH_SEPERATOR + BAL_GIT_IGNORE_VM, createTemplateContextForMeta(generatorProperties), directoryPath,
                BAL_GIT_IGNORE_FILE);
    }

    private TemplateContext createTemplateContextForMeta(Map<String, Object> generatorProperties) {

        TemplateContext templateContext = this.getNewTemplateContext();
        BallerinaProjectToolConfig config = (BallerinaProjectToolConfig) generatorProperties.get(CONFIG);
        templateContext.setProperty(CONFIG, config);
        templateContext.setProperty(META_CONFIG, config.getMetadataConfig());
        return templateContext;
    }
}
