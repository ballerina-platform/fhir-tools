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

package org.wso2.healthcare.cds.codegen.ballerina.tool;

import org.wso2.healthcare.cds.codegen.ballerina.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.cds.codegen.ballerina.tool.generator.BallerinaProjectGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.AbstractTool;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.CONFIG;

public class CdsBallerinaProjectTool extends AbstractTool {
    private BallerinaProjectToolConfig ballerinaProjectToolConfig;

    @Override
    public void initialize(ToolConfig toolConfig) throws CodeGenException {
        ballerinaProjectToolConfig = (BallerinaProjectToolConfig) toolConfig;
    }

    @Override
    public ToolContext getToolContext() throws CodeGenException {
        return null;
    }

    @Override
    public void setToolContext(ToolContext toolContext) throws CodeGenException {

    }

    @Override
    public TemplateGenerator execute(ToolContext toolContext) throws CodeGenException {
        String targetRoot = ballerinaProjectToolConfig.getTargetDir();
        String targetDirectory = targetRoot + File.separator;
        BallerinaProjectGenerator cdsBalGenerator = new BallerinaProjectGenerator(targetDirectory);
        Map<String, Object> generatorProperties = new HashMap<>();
        generatorProperties.put(CONFIG, ballerinaProjectToolConfig);
        cdsBalGenerator.setGeneratorProperties(generatorProperties);
        return cdsBalGenerator;
    }
}
