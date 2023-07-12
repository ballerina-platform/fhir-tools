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

import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;

import java.io.File;
import java.util.Map;

/**
 * Generator for Ballerina service test files.
 */
public class TestGenerator extends AbstractFHIRTemplateGenerator {

    public TestGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {

        String ballerinaAPI = generatorProperties.get("resourceType") + "API";
        String directoryPath = this.getTargetDir() + ballerinaAPI + File.separator + "tests" + File.separator;
        this.getTemplateEngine().generateOutputAsFile("template/balServiceTest.vm",
                createTemplateContextForTest(generatorProperties), directoryPath, "service_test.bal");
    }

    private TemplateContext createTemplateContextForTest(Map<String, Object> generatorProperties) {

        TemplateContext templateContext = this.getNewTemplateContext();
        BallerinaProjectToolConfig config = (BallerinaProjectToolConfig) generatorProperties.get("config");
        templateContext.setProperty("metaConfig", config.getMetadataConfig());
        templateContext.setProperty("resourceType", generatorProperties.get("resourceType") + "API");
        return templateContext;
    }
}
