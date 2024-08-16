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

package io.ballerina.health.cmd.cds;

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import io.ballerina.health.cmd.handler.Handler;
import io.ballerina.health.cmd.handler.HandlerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.*;

@CommandLine.Command(name = "cds", description = "Generates Ballerina service for provided cds hook definitions.")
public class CdsSubCmd implements BLauncherCmd {

    private final PrintStream printStream;
    private final boolean exitWhenFinish;
    private final String toolName = "cds";
    private final Path executionPath = Paths.get(System.getProperty("user.dir"));
    private Path targetOutputPath;

    @CommandLine.Option(names = {"--help", "-h", "?"}, usageHelp = true, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"-m", "--mode"}, description = "Execution mode. Only \"template\" option is supported.")
    private String mode;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Location of the generated Ballerina artifacts.")
    private String outputPath;

    @CommandLine.Option(names = {"--package-name"}, description = "Name of the Ballerina package")
    private String packageName;

    @CommandLine.Option(names = {"--org-name"}, description = "Organization name of the Ballerina package")
    private String orgName;

    @CommandLine.Option(names = {"--package-version"}, description = "version of the Ballerina package")
    private String packageVersion;

    @CommandLine.Parameters(description = "Custom arguments")
    private List<String> argList;

    public CdsSubCmd(PrintStream printStream, boolean exitWhenFinish) {
        this.printStream = printStream;
        this.exitWhenFinish = exitWhenFinish;
        LogManager.getLogManager().reset();
    }

    public CdsSubCmd() {
        this.printStream = System.out;
        this.exitWhenFinish = true;
        LogManager.getLogManager().reset();
    }

    @Override
    public void execute() {
        if (helpFlag) {
            Class<?> clazz = CdsSubCmd.class;
            ClassLoader classLoader = clazz.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(HealthCmdConstants.CMD_CDS_HELP_TEXT_FILENAME);
            if (inputStream != null) {
                try (InputStreamReader inputStreamREader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                     BufferedReader br = new BufferedReader(inputStreamREader)) {
                    String content = br.readLine();
                    printStream.append(content);
                    while ((content = br.readLine()) != null) {
                        printStream.append('\n').append(content);
                    }
                    return;
                } catch (IOException e) {
                    printStream.println(HealthCmdConstants.PrintStrings.HELP_ERROR);
                    HealthCmdUtils.throwLauncherException(e);
                }
            }
            printStream.println(HealthCmdConstants.PrintStrings.HELP_NOT_AVAILABLE);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (argList == null || argList.isEmpty()) {
            //at minimum arg count is 1 (spec path)
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_NUM_OF_ARGS);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        mode = CMD_MODE_TEMPLATE;

        if (this.engageSubCommand(argList)) {
            printStream.println(HealthCmdConstants.PrintStrings.TEMPLATE_GEN_SUCCESS + targetOutputPath);
        } else {
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_MODE);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
        }

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

    public boolean engageSubCommand(List<String> argList) {

        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put(CMD_OPTION_PACKAGE_NAME, packageName);
        argsMap.put(CMD_OPTION_ORG_NAME, orgName);
        argsMap.put(CMD_OPTION_PACKAGE_VERSION, packageVersion);
        getTargetOutputPath();
        //spec path is the last argument
        //resolved path from the input parameter
        Path cdsToolConfigFilePath;
        try {
            cdsToolConfigFilePath = HealthCmdUtils.getSpecificationPath(argList.get(argList.size() - 1), executionPath.toString());
        } catch (BallerinaHealthException e) {
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_SPEC_PATH);
            throw new BLauncherException();
        }
        Handler toolHandler;
        try {
            toolHandler = HandlerFactory.createHandler(toolName, mode, printStream, cdsToolConfigFilePath.toString());
        } catch (BallerinaHealthException e) {
            printStream.println(e);
            throw new BLauncherException();
        }

        toolHandler.setArgs(argsMap);
        return toolHandler.execute(cdsToolConfigFilePath.toString(), targetOutputPath.toString());
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
            targetOutputPath = Paths.get(targetOutputPath + File.separator + "generated-" + mode);
        }
    }
}
