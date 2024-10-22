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
import org.apache.commons.lang.StringUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CDS;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CDS_SUB_TOOL_DESCRIPTION;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_MODE_TEMPLATE;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_CDS_MODE_DESCRIPTION;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_CUSTOM_ARGS_DESCRIPTION;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_HELP;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_HELP_SHORTER_1;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_HELP_SHORTER_2;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_INPUT;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_INPUT_SHORTER;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_MODE;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_MODE_SHORTER;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_ORG_NAME;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_ORG_NAME_DESCRIPTION;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_OUTPUT;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_OUTPUT_DESCRIPTION;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_OUTPUT_SHORTER;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_PACKAGE_NAME;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_PACKAGE_NAME_DESCRIPTION;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_PACKAGE_VERSION;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_OPTION_PACKAGE_VERSION_DESCRIPTION;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.USER_DIR;


@CommandLine.Command(name = CDS, description = CDS_SUB_TOOL_DESCRIPTION)
public class CdsSubCmd implements BLauncherCmd {

    private final PrintStream printStream;
    private final boolean exitWhenFinish;
    private final String toolName = CDS;
    private final Path executionPath = Paths.get(System.getProperty(USER_DIR));
    private Path targetOutputPath;

    @CommandLine.Option(names = {CMD_OPTION_HELP, CMD_OPTION_HELP_SHORTER_1, CMD_OPTION_HELP_SHORTER_2}, usageHelp = true, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {CMD_OPTION_MODE_SHORTER, CMD_OPTION_MODE}, description = CMD_OPTION_CDS_MODE_DESCRIPTION)
    private String mode;

    @CommandLine.Option(names = {CMD_OPTION_OUTPUT_SHORTER, CMD_OPTION_OUTPUT}, description = CMD_OPTION_OUTPUT_DESCRIPTION)
    private String outputPath;

    @CommandLine.Option(names = {CMD_OPTION_PACKAGE_NAME}, description = CMD_OPTION_PACKAGE_NAME_DESCRIPTION)
    private String packageName;

    @CommandLine.Option(names = {CMD_OPTION_ORG_NAME}, description = CMD_OPTION_ORG_NAME_DESCRIPTION)
    private String orgName;

    @CommandLine.Option(names = {CMD_OPTION_PACKAGE_VERSION}, description = CMD_OPTION_PACKAGE_VERSION_DESCRIPTION)
    private String packageVersion;

    @CommandLine.Option(names = {CMD_OPTION_INPUT, CMD_OPTION_INPUT_SHORTER}, description = CMD_OPTION_PACKAGE_VERSION_DESCRIPTION)
    private String inputFilePath;

    @CommandLine.Parameters(description = CMD_OPTION_CUSTOM_ARGS_DESCRIPTION)
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
                        // System.lineSeparator() is equal to \n
                        printStream.append(System.lineSeparator()).append(content);
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

        if (inputFilePath == null) {
            printStream.println(HealthCmdConstants.PrintStrings.NO_INPUT_FILE_PATH);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }

        if (StringUtils.isEmpty(inputFilePath)) {
            printStream.println(HealthCmdConstants.PrintStrings.EMPTY_INPUT_FILE_PATH);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }

        if (!Files.exists(Path.of(inputFilePath))) {
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_INPUT_FILE_PATH);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }

        // By default, only template mode is supported for cds sub command
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
            cdsToolConfigFilePath = HealthCmdUtils.getSpecificationPath(inputFilePath, executionPath.toString());
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
