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
import org.wso2.healthcare.cds.codegen.ballerina.tool.model.BallerinaService;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;

import java.util.ArrayList;
import java.util.Map;

import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_CONFIG_TOML_FILE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_CONFIG_TOML_VM;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_TOML_FILE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.BAL_TOML_VM;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.CONFIG;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.KEYWORDS;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.META_CONFIG;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.SERVICE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.TEMPLATE_NAME;


/**
 * Generator for Ballerina.Toml file
 */
public class TomlGenerator extends AbstractFHIRTemplateGenerator {

    public TomlGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = getTargetDir();
        this.getTemplateEngine().generateOutputAsFile(CdsBallerinaProjectConstants.RESOURCE_PATH_TEMPLATES
                        + CdsBallerinaProjectConstants.RESOURCE_PATH_SEPARATOR + BAL_TOML_VM,
                createTemplateContextForToml(generatorProperties), directoryPath, BAL_TOML_FILE);

        this.getTemplateEngine().generateOutputAsFile(CdsBallerinaProjectConstants.RESOURCE_PATH_TEMPLATES
                        + CdsBallerinaProjectConstants.RESOURCE_PATH_SEPARATOR + BAL_CONFIG_TOML_VM,
                createTemplateContextForToml(generatorProperties), directoryPath, BAL_CONFIG_TOML_FILE);
    }

    private TemplateContext createTemplateContextForToml(Map<String, Object> generatorProperties) {
        TemplateContext templateContext = this.getNewTemplateContext();
        BallerinaProjectToolConfig config = (BallerinaProjectToolConfig) generatorProperties.get(CONFIG);
        templateContext.setProperty(META_CONFIG, config.getMetadataConfig());
        templateContext.setProperty(TEMPLATE_NAME, config.getMetadataConfig().getNamePrefix());
        templateContext.setProperty(KEYWORDS, new ArrayList<>(config.getMetadataConfig().getKeywords()));
        BallerinaService ballerinaService = (BallerinaService) generatorProperties.get(SERVICE);
        templateContext.setProperty(SERVICE, ballerinaService);
        return templateContext;
    }
}
