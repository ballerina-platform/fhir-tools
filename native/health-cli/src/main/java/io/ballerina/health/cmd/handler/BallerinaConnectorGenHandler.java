package io.ballerina.health.cmd.handler;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.health.cmd.core.config.HealthCmdConfig;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.ErrorMessages;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.Tool;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;

public class BallerinaConnectorGenHandler implements Handler {

    private JsonObject configJson;
    private PrintStream printStream;

    @Override
    public void init(PrintStream printStream, String configFilePath) {
        this.printStream = printStream;
        try {
            configJson = HealthCmdConfig.getParsedConfigFromStream(HealthCmdUtils.getResourceFile(
                    this.getClass(), HealthCmdConstants.CMD_CONNECTOR_CONFIG_FILENAME));
        } catch (BallerinaHealthException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void setArgs(Map<String, Object> argsMap) {

    }

    @Override
    public boolean execute(String configFilePath, String targetOutputPath) {
        // Holds the default configs
        // TODO: read from the configFilePath if provided
        JsonElement toolExecConfigs = null;
        if (configJson != null) {
            toolExecConfigs = configJson.getAsJsonObject("config");
        } else {
            printStream.println(ErrorMessages.CONFIG_PARSE_ERROR);
            HealthCmdUtils.exitError(true);
        }

        if (toolExecConfigs != null) {
            JsonObject toolExecConfig = toolExecConfigs.getAsJsonObject();

            ClassLoader classLoader = this.getClass().getClassLoader();
            String configClassName = "org.wso2.healthcare.fhir.ballerina.connectorgen.tool.config." +
                    "BallerinaConnectorGenToolConfig";
            Class<?> configClazz = null;
            Tool tool;
            TemplateGenerator connectorGenerator = null;
            try {
                configClazz = classLoader.loadClass(configClassName);

                ToolConfig toolConfigInstance = (ToolConfig) configClazz.getConstructor().newInstance();
                toolConfigInstance.setTargetDir(targetOutputPath);
                toolConfigInstance.setToolName(HealthCmdConstants.CMD_CONNECTOR);
                toolConfigInstance.configure(new JsonConfigType(toolExecConfig));

                String toolClassName = "org.wso2.healthcare.fhir.ballerina.connectorgen.tool.BallerinaConnectorGenTool";
                Class<?> toolClazz = classLoader.loadClass(toolClassName);
                tool = (Tool) toolClazz.getConstructor().newInstance();
                tool.initialize(toolConfigInstance);

                // No tool context is needed for connector generation
                connectorGenerator = tool.execute(null);
            } catch (ClassNotFoundException e) {
                printStream.println(ErrorMessages.TOOL_IMPL_NOT_FOUND + e.getMessage());
                HealthCmdUtils.throwLauncherException(e);
            } catch (InstantiationException | IllegalAccessException e) {
                printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
                HealthCmdUtils.throwLauncherException(e);
            } catch (NoSuchMethodException | InvocationTargetException e ) {
                throw new RuntimeException(e);
            } catch (CodeGenException e) {
                printStream.println(ErrorMessages.UNKNOWN_ERROR);
                printStream.println(e);
                HealthCmdUtils.throwLauncherException(e);
            }

            if (connectorGenerator != null) {
                try {
                    connectorGenerator.generate(null, connectorGenerator.getGeneratorProperties());
                } catch (CodeGenException e) {
                    printStream.println(ErrorMessages.UNKNOWN_ERROR + e.getMessage());
                    HealthCmdUtils.throwLauncherException(e);
                }
                return true;
            } else {
                printStream.println("Connector generator is not registered for the tool: " + HealthCmdConstants.CMD_CONNECTOR);
                printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
            }
        }
        return false;
    }
}
