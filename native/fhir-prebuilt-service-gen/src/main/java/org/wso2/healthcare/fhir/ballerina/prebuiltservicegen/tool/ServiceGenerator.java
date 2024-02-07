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
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator.MetaGenerator;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator.TomlGenerator;

import java.io.File;
import java.util.Calendar;
import java.util.Map;

/**
 * FHIR prebuilt service generator class.
 */
public class ServiceGenerator extends AbstractFHIRTemplateGenerator {
    public ServiceGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        //generation of fhir service files by reusing the template gen tool lib
        org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator.ServiceGenerator balServiceGenerator = new org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator.ServiceGenerator(this.getTargetDir());
        balServiceGenerator.generate(toolContext, generatorProperties);
        TomlGenerator tomlGenerator = new TomlGenerator(this.getTargetDir());
        tomlGenerator.generate(toolContext, generatorProperties);
        MetaGenerator metaFilesGenerator = new MetaGenerator(this.getTargetDir());
        metaFilesGenerator.generate(toolContext, generatorProperties);
        //generation of prebuilt service specific files.
        String directoryPath = generatorProperties.get("projectAPIPath") + File.separator;
        String initFileName = "connection_config.bal";
        String serviceUtilsFileName = "serviceUtils.bal";
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                File.separator + "connection_config.vm", createTemplateContext(
                generatorProperties), directoryPath, initFileName);
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                File.separator + "service_utils.vm", createTemplateContext(
                generatorProperties), directoryPath, serviceUtilsFileName);
    }

    private TemplateContext createTemplateContext(Map<String, Object> generatorProperties) {
        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty("serverName", generatorProperties.get("serverName"));
        templateContext.setProperty("currentYear", Calendar.getInstance().get(Calendar.YEAR));
        templateContext.setProperty("authMethod", generatorProperties.get("authMethod"));
        return templateContext;
    }
}
