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
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.FHIRProfile;

import java.io.File;
import java.util.*;

public class MetaGenerator extends AbstractFHIRTemplateGenerator {

    public MetaGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = generatorProperties.get("projectAPIPath") + File.separator;
        String apiConfigFileName = "api_config.bal";
        if (generatorProperties.containsKey("apiConfigFileName")) {
            apiConfigFileName = (String) generatorProperties.get("apiConfigFileName");
        }
        if (!generatorProperties.containsKey("disablePackageMd")) {
            this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                            File.separator + "packageMd.vm", createTemplateContextForMeta(generatorProperties), directoryPath,
                    "Package.md");
        }
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                File.separator + "gitignore.vm", createTemplateContextForMeta(generatorProperties), directoryPath,
                ".gitignore");
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                File.separator + "apiConfig.vm", createTemplateContextForMeta(generatorProperties), directoryPath,
                apiConfigFileName);
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
        templateContext.setProperty("sampleIGCamelCase", CaseUtils.toCamelCase(sampleIG, true,'-'));
        templateContext.setProperty("sampleIGLowerCase", sampleIG.toLowerCase());
        templateContext.setProperty("profileURLs", profileURLs);
        templateContext.setProperty("config", config);
        templateContext.setProperty("metaConfig", config.getMetadataConfig());
        templateContext.setProperty("service", service);
        templateContext.setProperty("apiName", generatorProperties.get("resourceType") + "API");
        templateContext.setProperty("templateName", config.getMetadataConfig().getNamePrefix() + "." +
                generatorProperties.get("resourceType").toString().toLowerCase());
        templateContext.setProperty("currentYear", Calendar.getInstance().get(Calendar.YEAR));
        //default api config
        templateContext.setProperty("apiConfName", "apiConfig");
        if (generatorProperties.containsKey("apiConfName")) {
            templateContext.setProperty("apiConfName", generatorProperties.get("apiConfName"));
        }

        Map<String, String> dependencies = (HashMap<String, String>) generatorProperties.get("dependencies");
        templateContext.setProperty("basePackage", dependencies.get("basePackage"));
        templateContext.setProperty("basePackageImportIdentifier", generatorProperties.get(
                "basePackageImportIdentifier"));
        templateContext.setProperty("servicePackageImportIdentifier", generatorProperties.get(
                "servicePackageImportIdentifier"));
        return templateContext;
    }
}
