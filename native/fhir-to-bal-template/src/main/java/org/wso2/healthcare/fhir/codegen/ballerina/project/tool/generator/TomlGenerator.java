/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator;

import org.wso2.healthcare.codegen.tooling.common.core.TemplateContext;
import org.wso2.healthcare.codegen.tooling.common.core.ToolContext;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.tool.lib.AbstractFHIRTemplateGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generator for Ballerina.Toml file.
 */
public class TomlGenerator extends AbstractFHIRTemplateGenerator {

    public TomlGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {

        String ballerinaAPI = generatorProperties.get("resourceType") + "API";
        String directoryPath = this.getTargetDir() + ballerinaAPI + File.separator;
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                        File.separator + "ballerinaToml.vm", createTemplateContextForToml(generatorProperties),
                directoryPath, "Ballerina.toml");
    }

    private TemplateContext createTemplateContextForToml(Map<String, Object> generatorProperties) {

        TemplateContext templateContext = this.getNewTemplateContext();
        BallerinaProjectToolConfig config = (BallerinaProjectToolConfig) generatorProperties.get("config");
        templateContext.setProperty("metaConfig", config.getMetadataConfig());
        templateContext.setProperty("dependencyList", config.getDependencyConfig());
        templateContext.setProperty("resourceType", generatorProperties.get("resourceType") + "API");
        templateContext.setProperty("templateName", config.getMetadataConfig().getNamePrefix() +
                generatorProperties.get("resourceType").toString().toLowerCase());
        templateContext.setProperty("keywords", this.generateKeywords(config,
                (BallerinaService) generatorProperties.get("service")));
        return templateContext;
    }

    private List<String> generateKeywords(BallerinaProjectToolConfig config, BallerinaService service) {
        List<String> keywords = new ArrayList<>(config.getMetadataConfig().getKeywords());
        keywords.add(service.getName());
        keywords.add(config.getFhirVersion());
        keywords.addAll(service.getIgs());
        return keywords;
    }
}
