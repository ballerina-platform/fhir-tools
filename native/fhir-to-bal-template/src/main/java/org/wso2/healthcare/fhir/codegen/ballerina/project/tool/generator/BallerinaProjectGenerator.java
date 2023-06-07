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

import org.wso2.healthcare.codegen.tooling.common.core.ToolContext;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.tool.lib.AbstractFHIRTemplateGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Generator class to wrap all the generator classes in Ballerina project generator.
 */
public class BallerinaProjectGenerator extends AbstractFHIRTemplateGenerator {

    public BallerinaProjectGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {

        BallerinaProjectToolConfig ballerinaProjectToolConfig = (BallerinaProjectToolConfig) generatorProperties.get(
                "config");
        Map<String, BallerinaService> serviceMap = (Map<String, BallerinaService>) generatorProperties.get(
                "serviceMap");
        //evaluate usage of ? typed map as generator properties.

        for (Map.Entry<String, BallerinaService> entry : serviceMap.entrySet()) {
            Map<String, Object> projectProperties = new HashMap<>();
            projectProperties.put("service", entry.getValue());
            projectProperties.put("resourceType", entry.getKey());
            projectProperties.put("config", ballerinaProjectToolConfig);
            ServiceGenerator balServiceGenerator = new ServiceGenerator(this.getTargetDir());
            balServiceGenerator.generate(toolContext, projectProperties);
            ProfileGenerator profileGenerator = new ProfileGenerator(this.getTargetDir());
            profileGenerator.generate(toolContext, projectProperties);
            TomlGenerator tomlGenerator = new TomlGenerator(this.getTargetDir());
            tomlGenerator.generate(toolContext, projectProperties);
            UtilGenerator utilGenerator = new UtilGenerator(this.getTargetDir());
            utilGenerator.generate(toolContext, projectProperties);
            MetaGenerator metaGenerator = new MetaGenerator(this.getTargetDir());
            metaGenerator.generate(toolContext, projectProperties);
            TestGenerator testGenerator = new TestGenerator(this.getTargetDir());
            testGenerator.generate(toolContext, projectProperties);
        }

    }
}
