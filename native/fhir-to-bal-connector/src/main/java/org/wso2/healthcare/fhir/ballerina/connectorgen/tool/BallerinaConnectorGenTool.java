/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.healthcare.fhir.ballerina.connectorgen.tool;

import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.config.BallerinaConnectorGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.generator.BallerinaConnectorGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTool;

import java.util.HashMap;
import java.util.Map;

public class BallerinaConnectorGenTool extends AbstractFHIRTool {

    private BallerinaConnectorGenToolConfig connectorGenToolConfig;

    @Override
    public void initialize(ToolConfig toolConfig) throws CodeGenException {
        this.connectorGenToolConfig = (BallerinaConnectorGenToolConfig) toolConfig;
    }

    @Override
    public BallerinaConnectorGenerator execute(ToolContext toolContext) throws CodeGenException {

        String targetRoot = connectorGenToolConfig.getTargetDir();

        BallerinaConnectorGenerator connectorGenerator = new BallerinaConnectorGenerator(targetRoot);
        Map<String, Object> properties = new HashMap<>();

        properties.put("toolConfig", connectorGenToolConfig);
        connectorGenerator.setGeneratorProperties(properties);

        return connectorGenerator;
    }

}
