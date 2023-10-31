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

import com.google.gson.JsonElement;
import io.ballerina.cli.BLauncherCmd;
import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import io.ballerina.health.cmd.core.utils.JsonTypeConverter;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
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

    @CommandLine.Option(names = "--ig-package", description = "Resource package name for the templates to be generated")
    private String resourcePackage;
    @CommandLine.Option(names = "--dependency", converter = JsonTypeConverter.class, description = "custom dependency to be added")
    private JsonElement dependency;


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
            InputStream inputStream = classLoader.getResourceAsStream(HealthCmdConstants.CMD_HELPTEXT_FILENAME);
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
        if (this.engageSubCommand(argList)) {
            printStream.println("Ballerina FHIR package generation completed successfully. Generated " +
                    mode + " can be found at " + targetOutputPath);
        } else {
            printStream.println("Invalid mode received for FHIR tool command.");
            printStream.println("Try bal health --help for more information.");
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
        argsMap.put("--ig-package", resourcePackage);
        argsMap.put("--dependency", dependency);
        getTargetOutputPath();
        //spec path is the last argument
        try {
            specificationPath = HealthCmdUtils.validateAndSetSpecificationPath(argList.get(argList.size() - 1), executionPath.toString());
        } catch (BallerinaHealthException e) {
            printStream.println("Invalid specification path received for FHIR tool command.");
            throw new BLauncherException();
        }
        Handler toolHandler = null;
        try {
            toolHandler = HandlerFactory.createHandler(mode, printStream, specificationPath.toString());
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
