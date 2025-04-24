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

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import io.ballerina.health.cmd.handler.Handler;
import io.ballerina.health.cmd.handler.HandlerFactory;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_MODE_PACKAGE;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.CMD_MODE_TEMPLATE;

@CommandLine.Command(name = "fhir", description = "Generates Ballerina service/client for FHIR contract " +
        "for Ballerina service.")
public class FhirSubCmd implements BLauncherCmd {

    private final PrintStream printStream;
    private final boolean exitWhenFinish;
    private final String toolName = "fhir";
    private final Path executionPath = Paths.get(System.getProperty("user.dir"));
    private final String resourceHome;
    private Path targetOutputPath;

    //resolved path from the input parameter
    private Path specificationPath;
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

    @CommandLine.Option(names = {"--package-version"}, description = "version of the Ballerina package")
    private String packageVersion;

    @CommandLine.Option(names = "--included-profile", description = "Profiles to be included in the template")
    private String[] includedProfiles;

    @CommandLine.Option(names = "--excluded-profile", description = "Profiles to be excluded in the template")
    private String[] excludedProfiles;

    @CommandLine.Option(names = "--dependent-package", description = "Dependent package name for the templates to be generated")
    private String dependentPackage;

    @CommandLine.Option(names = "--dependent-ig", description = "Dependent IG base URL and respective fully qualified Ballerina package name")
    private String[] dependentIgs;


    @CommandLine.Parameters(description = "Custom arguments")
    private List<String> argList;

    public FhirSubCmd(PrintStream printStream, boolean exitWhenFinish) {
        this.printStream = printStream;
        this.exitWhenFinish = exitWhenFinish;
        this.resourceHome = HealthCmdUtils.getRuntimeResourcePath();
        LogManager.getLogManager().reset();
    }

    public FhirSubCmd() {
        this.printStream = System.out;
        this.exitWhenFinish = true;
        this.resourceHome = HealthCmdUtils.getRuntimeResourcePath();
        LogManager.getLogManager().reset();
    }

    @Override
    public void execute() {
        if (helpFlag) {
            Class<?> clazz = FhirSubCmd.class;
            ClassLoader classLoader = clazz.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(HealthCmdConstants.CMD_HELP_TEXT_FILENAME);
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
                    printStream.println(HealthCmdConstants.PrintStrings.HELP_NOT_AVAILABLE);
                    HealthCmdUtils.throwLauncherException(e);
                }
            }
            printStream.println(HealthCmdConstants.PrintStrings.HELP_ERROR);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (argList == null || argList.isEmpty()) {
            //at minimum arg count is 1 (spec path)
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_NUM_OF_ARGS);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (mode == null || mode.isEmpty()) {
            //mode is required param
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_MODE);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (CMD_MODE_PACKAGE.equals(mode) && (packageName == null || packageName.isEmpty())) {
            // package name is a required param in package mode
            printStream.println(HealthCmdConstants.PrintStrings.PKG_NAME_REQUIRED);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (CMD_MODE_TEMPLATE.equals(mode) && (dependentPackage == null || dependentPackage.isEmpty())) {
            // dependent package is a required param in template mode
            printStream.println(HealthCmdConstants.PrintStrings.DEPENDENT_REQUIRED);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (dependentPackage != null && !dependentPackage.isEmpty()) {
            // regex matching ballerinax/health.fhir.r4
            if (!dependentPackage.matches("^(?!.*__)[a-zA-Z0-9][a-zA-Z0-9_]+[a-zA-Z0-9]/[a-zA-Z0-9][a-zA-Z0-9._]+[a-zA-Z0-9]$")) {
                printStream.println(HealthCmdConstants.PrintStrings.DEPENDENT_INCORRECT);
                printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
                HealthCmdUtils.exitError(exitWhenFinish);
            }
        }
        if (includedProfiles != null && excludedProfiles != null) {
            printStream.println(HealthCmdConstants.PrintStrings.INCLUDED_EXCLUDED_TOGETHER);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (this.engageSubCommand(argList)) {
            if (CMD_MODE_TEMPLATE.equals(mode)) {
                printStream.println(HealthCmdConstants.PrintStrings.TEMPLATE_GEN_SUCCESS_MESSAGE + targetOutputPath);
            } else {
                printStream.println(HealthCmdConstants.PrintStrings.PKG_GEN_SUCCESS + targetOutputPath);
            }
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
        argsMap.put("--package-name", packageName);
        argsMap.put("--org-name", orgName);
        argsMap.put("--package-version", packageVersion);
        argsMap.put("--included-profile", includedProfiles);
        argsMap.put("--excluded-profile", excludedProfiles);
        argsMap.put("--dependent-package", dependentPackage);
        argsMap.put("--dependent-ig", dependentIgs);
        getTargetOutputPath();
        //spec path is the last argument
        try {
            specificationPath = HealthCmdUtils.validateAndSetSpecificationPath(argList.get(argList.size() - 1), executionPath.toString());
        } catch (BallerinaHealthException e) {
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_SPEC_PATH);
            throw new BLauncherException();
        }
        Handler toolHandler = null;
        try {
            toolHandler = HandlerFactory.createHandler(toolName, mode, printStream, specificationPath.toString());
        } catch (BallerinaHealthException e) {
            printStream.println(e);
            throw new BLauncherException();
        }

        toolHandler.setArgs(argsMap);
        return toolHandler.execute(specificationPath.toString(), targetOutputPath.toString());
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
