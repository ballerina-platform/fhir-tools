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
package org.wso2.healthcare.fhir.ballerina.packagegen.tool.templategen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.PackageTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Template generator for FHIR datatypes to be used to generate datatypes source file.
 */
public class DatatypeTemplateGenerator extends AbstractFHIRTemplateGenerator {

    private static final Log LOG = LogFactory.getLog(DatatypeTemplateGenerator.class);

    public DatatypeTemplateGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
        LOG.debug("Datatype Template Generator Initiated");
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        BallerinaPackageGenToolConfig toolConfig = (BallerinaPackageGenToolConfig) generatorProperties.get("toolConfig");
        String packagePath = this.getTargetDir() + File.separator + toolConfig.getPackageConfig().getName();
        Map<String, DatatypeTemplateContext> datatypeContexts =
                (Map<String, DatatypeTemplateContext>)generatorProperties.get("datatypeContext");
        if (datatypeContexts == null) {
            throw new CodeGenException("Datatype context is not available.");
        }
        PackageTemplateContext packageTemplateContext = (PackageTemplateContext) generatorProperties.get("packageContext");
        List<String> importList = Collections.singletonList(packageTemplateContext.getBasePackageName());
        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty("util", GeneratorUtils.getInstance());
        templateContext.setProperty("licenseYear", ToolConstants.LICENSE_YEAR);
        templateContext.setProperty("datatypeContext", datatypeContexts);
        templateContext.setProperty("imports", importList);

        if (!datatypeContexts.isEmpty()) {
            String filePath = CommonUtil.generateFilePath(packagePath, "", "datatypes.bal");
            this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH +
                    ToolConstants.RESOURCE_PATH_SEPERATOR + "fhir_extended_datatypes.vm", templateContext, "", filePath);
        }
    }
}
