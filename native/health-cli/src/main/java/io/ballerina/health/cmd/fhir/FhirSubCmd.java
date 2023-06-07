/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.health.cmd.fhir;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.cli.BLauncherCmd;
import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.config.HealthCmdConfig;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.ErrorMessages;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import org.apache.commons.lang3.StringUtils;
import org.wso2.healthcare.codegen.tooling.common.config.ToolConfig;
import org.wso2.healthcare.codegen.tooling.common.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tooling.common.core.Tool;
import org.wso2.healthcare.codegen.tooling.common.core.ToolContext;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.codegen.tooling.common.model.JsonConfigType;
import org.wso2.healthcare.fhir.codegen.tool.lib.config.FHIRToolConfig;
import org.wso2.healthcare.fhir.codegen.tool.lib.core.FHIRTool;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "fhir", description = "Generates Ballerina service/client for FHIR contract for Ballerina service.")
public class FhirSubCmd implements BLauncherCmd {
    private final PrintStream printStream;
    private final boolean exitWhenFinish;
    private final String toolName = "fhir";
    private final Path executionPath = Paths.get(System.getProperty("user.dir"));
    private Path targetOutputPath;

    //resolved path from the input parameter
    private Path specificationPath;

    //input parameter for specification path
    private String specPathParam;

    private String resourceHome;
    private JsonObject configJson = null;
    private JsonObject defaultConfigJson = null;
    @CommandLine.Option(names = {"--help", "-h", "?"}, usageHelp = true, hidden = true)
    private boolean helpFlag;


    @CommandLine.Option(names = {"-m", "--mode"}, description = "Execution mode. Only \"template\" and " +
            "\"package\" options are supported.")
    private String mode;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Location of the generated Ballerina artifacts.")
    private String outputPath;

    @CommandLine.Option(names = {"-c", "--config"}, description = "Path to the tool configuration file.")
    private String configPath;

    @CommandLine.Parameters(description = "User name")
    private List<String> argList;

    public FhirSubCmd(PrintStream printStream, boolean exitWhenFinish) {
        this.printStream = printStream;
        this.exitWhenFinish = exitWhenFinish;
        buildConfig(printStream);
        this.resourceHome = HealthCmdUtils.getRuntimeResourcePath();
    }

    public FhirSubCmd() {
        this.printStream = System.out;
        this.exitWhenFinish = true;
        buildConfig(printStream);
        this.resourceHome = HealthCmdUtils.getRuntimeResourcePath();
    }

    @Override
    public void execute() {

        if (helpFlag) {
            InputStream inputStream = ClassLoader.getSystemResourceAsStream("ballerina-health.help");
            if (inputStream != null) {
                try (InputStreamReader inputStreamREader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                     BufferedReader br = new BufferedReader(inputStreamREader)) {
                    String content = br.readLine();
                    printStream.append(content);
                    while ((content = br.readLine()) != null) {
                        printStream.append('\n').append(content);
                    }
                } catch (IOException e) {
                    printStream.println("Helper text is not available.");
                    HealthCmdUtils.exitError(exitWhenFinish);;
                }
                return;
            }
        }

        if (argList.isEmpty()) {
            //at minimum arg count is 1 (spec path)
            printStream.println("Invalid number of arguments received for FHIR !\n try bal health --help for more information.");
            return;
        }
        this.engageSubCommand(argList);
        HealthCmdUtils.exitError(exitWhenFinish);

    }

    @Override
    public String getName() {
        return toolName;
    }

    @Override
    public void printLongDesc(StringBuilder stringBuilder) {

    }

    @Override
    public void printUsage(StringBuilder stringBuilder) {

    }

    @Override
    public void setParentCmdParser(CommandLine commandLine) {

    }

    private void engageChildTemplateGenerators(TemplateGenerator templateGenerator, ToolContext context,
                                               Map<String, Object> properties) throws CodeGenException {
        if (templateGenerator != null) {
            templateGenerator.generate(context, properties);
            engageChildTemplateGenerators(templateGenerator.getChildTemplateGenerator(), context, properties);
        }
    }

    public void engageSubCommand(List<String> argList) {

        //spec path is the last argument
        specPathParam = argList.get(argList.size() - 1);
        getTargetOutputPath();
        getSpecificationPath();

        if (!StringUtils.isEmpty(configPath)) {
            //override default configs with user provided configs
            try {
                configJson = HealthCmdConfig.getParsedConfigFromPath(Paths.get(configPath));
            } catch (BallerinaHealthException e) {
                //todo: handle properly. use Bal Utils
                throw new BLauncherException();
            }
        } else {
            configJson = defaultConfigJson;
        }

        JsonElement toolExecConfigs;
        JsonArray toolExecConfigArr = null;
        if (configJson != null) {
            toolExecConfigs = configJson.getAsJsonObject("fhir").get("tools");
            if (toolExecConfigs != null) {
                toolExecConfigArr = toolExecConfigs.getAsJsonArray();
            }
        } else {
            printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
            HealthCmdUtils.exitError(this.exitWhenFinish);
        }

        FHIRToolConfig fhirToolConfig = new FHIRToolConfig();

        if (configJson != null) {
            //default configs will be used.
            JsonConfigType toolConfig = null;
            FHIRTool fhirToolLib = null;
            try {
                toolConfig = new JsonConfigType(configJson);
                fhirToolLib = new FHIRTool();
                fhirToolConfig.configure(toolConfig);
                fhirToolConfig.setSpecBasePath(specificationPath.toString());
                fhirToolLib.initialize(fhirToolConfig);
            } catch (CodeGenException e) {
                //todo: verify if internal logging is needed
//                LOG.error("Error while initializing tool lib configs.", e);
                printStream.println(ErrorMessages.LIB_INITIALIZING_FAILED + Arrays.toString(e.getStackTrace()) + e.getMessage());
                HealthCmdUtils.exitError(this.exitWhenFinish);
            }

            for (JsonElement jsonElement : toolExecConfigArr) {
                JsonObject toolExecConfig = jsonElement.getAsJsonObject();
                String configClassName = toolExecConfig.get("configClass").getAsString();
                String toolClassName = toolExecConfig.get("toolClass").getAsString();
                String name = toolExecConfig.get("name").getAsString();
                String command = toolExecConfig.get("command").getAsString();
                Tool tool;
                TemplateGenerator mainTemplateGenerator = null;
                if (mode != null && !mode.equals(command)) {
                    continue;
                }
                try {
                    Class<?> configClazz = Class.forName(configClassName);
                    Class<?> toolClazz = Class.forName(toolClassName);
                    ToolConfig toolConfigInstance = (ToolConfig) configClazz.newInstance();
                    toolConfigInstance.setTargetDir(targetOutputPath.toString());
                    toolConfigInstance.setToolName(name);
                    JsonArray tools = toolConfig.getConfigObj().getAsJsonObject("fhir").get("tools").getAsJsonArray();
                    for (JsonElement element : tools) {
                        JsonElement toolName = element.getAsJsonObject().get("name");
                        if (toolName.getAsString().equals(name)) {
                            toolConfigInstance.configure(new JsonConfigType(element.getAsJsonObject().getAsJsonObject("config")));
                        }
                    }

                    tool = (Tool) toolClazz.newInstance();
                    tool.initialize(toolConfigInstance);
                    fhirToolLib.getToolImplementations().putIfAbsent(name, tool);
                    mainTemplateGenerator = tool.execute(fhirToolLib.getToolContext());
                } catch (ClassNotFoundException e) {
                    printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
                    HealthCmdUtils.exitError(this.exitWhenFinish);
                } catch (InstantiationException | IllegalAccessException e) {
                    printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
                    HealthCmdUtils.exitError(this.exitWhenFinish);
                } catch (CodeGenException e) {
                    printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
                    HealthCmdUtils.exitError(this.exitWhenFinish);
                }
                if (mainTemplateGenerator != null) {
                    try {
                        mainTemplateGenerator.generate(fhirToolLib.getToolContext(),
                                mainTemplateGenerator.getGeneratorProperties());
                        TemplateGenerator childTemplateGenerator = mainTemplateGenerator.getChildTemplateGenerator();
                        engageChildTemplateGenerators(childTemplateGenerator, fhirToolLib.getToolContext(),
                                mainTemplateGenerator.getGeneratorProperties());
                    } catch (CodeGenException e) {
                        printStream.println(ErrorMessages.TOOL_EXECUTION_FAILED);
                        HealthCmdUtils.exitError(this.exitWhenFinish);
                    }
                } else {
                    printStream.println("Template generator is not registered for the tool: " + name);
                    printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
                    HealthCmdUtils.exitError(this.exitWhenFinish);
                }
            }
        }
    }

    /**
     * This util is to get the output Path.
     */
    private void getTargetOutputPath() {
        targetOutputPath = executionPath;
        if (this.outputPath != null) {
            if (Paths.get(outputPath).isAbsolute()) {
                targetOutputPath = Paths.get(outputPath);
            } else {
                targetOutputPath = Paths.get(targetOutputPath.toString(), outputPath);
            }
        } else {
            targetOutputPath = Paths.get(targetOutputPath.toString(), "/gen");
        }
    }

    /**
     * This util is to get the output Path.
     */
    private void getSpecificationPath() {
        specificationPath = executionPath;
        if (this.specPathParam != null) {
            if (Paths.get(specPathParam).isAbsolute()) {
                specificationPath = Paths.get(specPathParam);
            } else {
                specificationPath = Paths.get(specificationPath.toString(), specPathParam);
            }
        }
    }

    private InputStream getResourceFile(String fileName) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream ioStream = classLoader.getResourceAsStream(fileName);

        if (ioStream == null) {
            throw new IllegalArgumentException("tool-config.json" + " is not found");
        }
        return ioStream;
    }

    private void buildConfig(PrintStream printStream) {
        //using default config file
        try {
            defaultConfigJson = HealthCmdConfig.getParsedConfigFromStream(getResourceFile(HealthCmdConstants.CMD_CONFIG_FILENAME));
        } catch (BallerinaHealthException e) {
            printStream.println(ErrorMessages.LIB_INITIALIZING_FAILED + Arrays.toString(e.getStackTrace()) + e.getMessage());
        }

    }
}
