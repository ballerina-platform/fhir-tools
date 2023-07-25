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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.cli.BLauncherCmd;
import io.ballerina.health.cmd.core.config.HealthCmdConfig;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.ErrorMessages;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import org.apache.commons.lang3.StringUtils;
import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.Tool;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.config.FHIRToolConfig;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.FHIRTool;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

@CommandLine.Command(name = "fhir", description = "Generates Ballerina service/client for FHIR contract " +
        "for Ballerina service.")
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

    private final String resourceHome;
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

    @CommandLine.Option(names = {"--package-name"}, description = "Name of the Ballerina package")
    private String packageName;

    @CommandLine.Option(names = {"--org-name"}, description = "Organization name of the Ballerina package")
    private String orgName;

    @CommandLine.Option(names = {"--ig-name"}, description = "Implementation guide name")
    private String igName;

    @CommandLine.Option(names = {"--ig-code"}, description = "Implementation guide code")
    private String igCode;

    @CommandLine.Parameters(description = "Custom arguments")
    private List<String> argList;

    public FhirSubCmd(PrintStream printStream, boolean exitWhenFinish) {
        this.printStream = printStream;
        this.exitWhenFinish = exitWhenFinish;
        buildConfig(printStream);
        this.resourceHome = HealthCmdUtils.getRuntimeResourcePath();
        LogManager.getLogManager().reset();
    }

    public FhirSubCmd() {
        this.printStream = System.out;
        this.exitWhenFinish = true;
        buildConfig(printStream);
        this.resourceHome = HealthCmdUtils.getRuntimeResourcePath();
        LogManager.getLogManager().reset();
    }

    @Override
    public void execute() {
        if (helpFlag) {
            Class<?> clazz = FhirSubCmd.class;
            ClassLoader classLoader = clazz.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(HealthCmdConstants.CMD_HELPTEXT_FILENAME);
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
                    HealthCmdUtils.throwLauncherException(e);
                }
            }
            printStream.println("An Error occurred internally while fetching the Help text.");
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (argList == null || argList.isEmpty()) {
            //at minimum arg count is 1 (spec path)
            printStream.println("Invalid number of arguments received for FHIR tool command.");
            printStream.println("Try bal health --help for more information.");
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (mode == null || mode.isEmpty()) {
            //mode is required param
            printStream.println("Invalid mode received for FHIR tool command.");
            printStream.println("Try bal health --help for more information.");
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        this.engageSubCommand(argList);
        printStream.println("Ballerina FHIR package generation completed successfully. Generated packages can be found "
                + "at " + targetOutputPath);
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

        getTargetOutputPath();
        //spec path is the last argument
        validateAndSetSpecificationPath(argList.get(argList.size() - 1));

        if (!StringUtils.isEmpty(configPath)) {
            //override default configs with user provided configs
            try {
                configJson = HealthCmdConfig.getParsedConfigFromPath(Paths.get(configPath));
            } catch (BallerinaHealthException e) {
                printStream.println(ErrorMessages.CONFIG_ACCESS_FAILED + e.getMessage());
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
            printStream.println(ErrorMessages.CONFIG_PARSE_ERROR);
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

                //override default configs for package-gen mode with user provided configs
                handleSpecificationPathAndOverride(fhirToolConfig, specificationPath);

                fhirToolConfig.setSpecBasePath(specificationPath.toString());
                fhirToolLib.initialize(fhirToolConfig);
            } catch (CodeGenException e) {
                printStream.println(ErrorMessages.LIB_INITIALIZING_FAILED + Arrays.toString(e.getStackTrace())
                        + e.getMessage());
                HealthCmdUtils.throwLauncherException(e);
            } catch (BallerinaHealthException e) {
                printStream.println(ErrorMessages.ARG_VALIDATION_FAILED + e.getMessage());
                HealthCmdUtils.throwLauncherException(e);
            }

            for (JsonElement jsonElement : toolExecConfigArr) {
                JsonObject toolExecConfig = jsonElement.getAsJsonObject();
                String configClassName = toolExecConfig.get("configClass").getAsString();
                String toolClassName = toolExecConfig.get("toolClass").getAsString();
                String name = toolExecConfig.get("name").getAsString();
                String command = toolExecConfig.get("command").getAsString();
                Tool tool;
                TemplateGenerator mainTemplateGenerator = null;
                if (!mode.equals(command)) {
                    continue;
                }
                try {
                    Class<?> configClazz = Class.forName(configClassName);
                    Class<?> toolClazz = Class.forName(toolClassName);
                    ToolConfig toolConfigInstance = (ToolConfig) configClazz.newInstance();
                    toolConfigInstance.setTargetDir(targetOutputPath.toString());
                    toolConfigInstance.setToolName(name);
                    JsonArray tools = toolConfig.getConfigObj().getAsJsonObject("fhir").
                            get("tools").getAsJsonArray();
                    for (JsonElement element : tools) {
                        JsonElement toolName = element.getAsJsonObject().get("name");
                        if (toolName.getAsString().equals(name)) {
                            toolConfigInstance.configure(new JsonConfigType(element.getAsJsonObject().
                                    getAsJsonObject("config")));
                        }
                    }

                    //override default configs for package-gen mode with user provided configs
                    if (command.equals("package")) {
                        if (packageName != null && !packageName.isEmpty()) {
                            JsonElement overrideConfig = new Gson().toJsonTree(packageName);
                            toolConfigInstance.overrideConfig("packageConfig.name", overrideConfig);
                        } else if (igName != null && !igName.isEmpty()) {
                            JsonElement overrideConfig = new Gson().toJsonTree(igName);
                            toolConfigInstance.overrideConfig("packageConfig.name.append", overrideConfig);
                        } else {
                            JsonElement overrideConfig = new Gson().toJsonTree(HealthCmdUtils.getDirectories(specificationPath).get(0));
                            toolConfigInstance.overrideConfig("packageConfig.name.append", overrideConfig);
                        }
                        if (orgName != null && !orgName.isEmpty()) {
                            JsonElement overrideConfig = new Gson().toJsonTree(orgName);
                            toolConfigInstance.overrideConfig("packageConfig.org", overrideConfig);
                        }
                    }

                    tool = (Tool) toolClazz.newInstance();
                    tool.initialize(toolConfigInstance);
                    fhirToolLib.getToolImplementations().putIfAbsent(name, tool);
                    mainTemplateGenerator = tool.execute(fhirToolLib.getToolContext());
                } catch (ClassNotFoundException e) {
                    printStream.println(ErrorMessages.TOOL_IMPL_NOT_FOUND + e.getMessage());
                    HealthCmdUtils.throwLauncherException(e);
                } catch (InstantiationException | IllegalAccessException e) {
                    printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
                    HealthCmdUtils.throwLauncherException(e);
                } catch (CodeGenException e) {
                    printStream.println(ErrorMessages.UNKNOWN_ERROR);
                    HealthCmdUtils.throwLauncherException(e);
                }
                if (mainTemplateGenerator != null) {
                    try {
                        mainTemplateGenerator.generate(fhirToolLib.getToolContext(),
                                mainTemplateGenerator.getGeneratorProperties());
                        TemplateGenerator childTemplateGenerator = mainTemplateGenerator.getChildTemplateGenerator();
                        engageChildTemplateGenerators(childTemplateGenerator, fhirToolLib.getToolContext(),
                                mainTemplateGenerator.getGeneratorProperties());
                    } catch (CodeGenException e) {
                        printStream.println(ErrorMessages.UNKNOWN_ERROR + e.getMessage());
                        HealthCmdUtils.throwLauncherException(e);
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
            targetOutputPath = Paths.get(targetOutputPath.toString());
        }
    }

    /**
     * This util is to get the output Path.
     */
    private void validateAndSetSpecificationPath(String specPathParam) {
        if (specPathParam != null) {
            Path path = Paths.get(specPathParam);
            if (path.isAbsolute()) {
                specificationPath = path;
            } else {
                specificationPath = Paths.get(executionPath.toString(), specPathParam);
            }
            if (!Files.isDirectory(specificationPath)) {
                printStream.println("Cannot find valid spec path pointed. Please check the path "
                        + specPathParam + " is valid.");
                HealthCmdUtils.exitError(exitWhenFinish);
            }
        }
    }

    private InputStream getResourceFile(String fileName) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream ioStream = classLoader.getResourceAsStream(fileName);

        if (ioStream == null) {
            throw new IllegalArgumentException(HealthCmdConstants.CMD_CONFIG_FILEPATH + " is not found");
        }
        return ioStream;
    }

    private void buildConfig(PrintStream printStream) {
        //using default config file
        try {
            defaultConfigJson = HealthCmdConfig.getParsedConfigFromStream(getResourceFile(
                    HealthCmdConstants.CMD_CONFIG_FILENAME));
        } catch (BallerinaHealthException e) {
            printStream.println(ErrorMessages.LIB_INITIALIZING_FAILED + Arrays.toString(e.getStackTrace()) +
                    e.getMessage());
        }
    }

    private void handleSpecificationPathAndOverride(FHIRToolConfig fhirToolConfig, Path specificationPath)
            throws BallerinaHealthException {

        if (igName == null || igName.isEmpty()) {
            igName = specificationPath.toString().split("/")[specificationPath.toString().length()];
        }
        if (Files.exists(specificationPath)) {
            fhirToolConfig.overrideConfig("FHIRImplementationGuides", HealthCmdUtils.getIGConfigElement(
                    igName, igCode));
        } else {
            printStream.println("No spec files found in the given path.");
            HealthCmdUtils.exitError(this.exitWhenFinish);
        }

    }
}
