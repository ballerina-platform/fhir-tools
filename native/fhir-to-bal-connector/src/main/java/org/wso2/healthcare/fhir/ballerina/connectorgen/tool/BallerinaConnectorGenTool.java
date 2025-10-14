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
