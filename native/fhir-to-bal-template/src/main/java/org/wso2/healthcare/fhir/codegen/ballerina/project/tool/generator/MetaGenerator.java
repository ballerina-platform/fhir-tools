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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator;

import org.apache.commons.text.CaseUtils;
import org.wso2.healthcare.codegen.tooling.common.core.TemplateContext;
import org.wso2.healthcare.codegen.tooling.common.core.ToolContext;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.FHIRProfile;
import org.wso2.healthcare.fhir.codegen.tool.lib.AbstractFHIRTemplateGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class generates the meta files of given FHIR Ballerina project.
 */
public class MetaGenerator extends AbstractFHIRTemplateGenerator {

    public MetaGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {

        String ballerinaAPI = generatorProperties.get("resourceType") + "API";
        String directoryPath = this.getTargetDir() + ballerinaAPI + File.separator;
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                        File.separator + "configToml.vm", createTemplateContextForMeta(generatorProperties),
                directoryPath, "Config.toml");
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                        File.separator + "packageMd.vm", createTemplateContextForMeta(generatorProperties),
                directoryPath, "Package.md");
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                        File.separator + "gitignore.vm", createTemplateContextForMeta(generatorProperties),
                directoryPath, ".gitignore");
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                        File.separator + "apiConfig.vm", createTemplateContextForMeta(generatorProperties),
                directoryPath, "api_config.bal");
    }

    private TemplateContext createTemplateContextForMeta(Map<String, Object> generatorProperties) {

        TemplateContext templateContext = this.getNewTemplateContext();
        BallerinaProjectToolConfig config = (BallerinaProjectToolConfig) generatorProperties.get("config");
        BallerinaService service = (BallerinaService) generatorProperties.get("service");
        Set<String> igURLs = new HashSet<>();
        List<String> profileURLs = new ArrayList<>();
        String sampleIG = "international";

        for (FHIRProfile profile : service.getProfileList()) {
            profileURLs.add(profile.getUrl());
            igURLs.add(profile.getUrl().split("/StructureDefinition")[0]);
            sampleIG = profile.getIgName();
        }
        templateContext.setProperty("igURLs", igURLs);
        templateContext.setProperty("sampleIGCamelCase", CaseUtils.toCamelCase(sampleIG, true, '-'));
        templateContext.setProperty("sampleIGLowerCase", sampleIG.toLowerCase());
        templateContext.setProperty("profileURLs", profileURLs);
        templateContext.setProperty("config", config);
        templateContext.setProperty("metaConfig", config.getMetadataConfig());
        templateContext.setProperty("service", service);
        templateContext.setProperty("apiName", generatorProperties.get("resourceType") + "API");
        templateContext.setProperty("templateName", config.getMetadataConfig().getNamePrefix() +
                generatorProperties.get("resourceType").toString().toLowerCase());
        return templateContext;
    }
}
