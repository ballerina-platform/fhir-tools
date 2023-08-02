/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package io.ballerina.health.cmd;

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import io.ballerina.health.cmd.fhir.FhirSubCmd;
import io.ballerina.health.cmd.hl7.Hl7SubCmd;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@CommandLine.Command(
        name = "health",
        description = "Ballerina Health Artifact Generator Tool",
        subcommands = {
                FhirSubCmd.class,
                Hl7SubCmd.class
        }
)
public class HealthCmd implements BLauncherCmd {

    private final PrintStream printStream;
    private final String toolName = "health";
    @CommandLine.Option(names = {"--help", "-h", "?"}, usageHelp = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"--version", "-v"})
    private boolean toolVersion;

    @CommandLine.Parameters(description = "Options for the sub commands")
    private List<String> argList;

    private String specPath;
    private String subCommand;

    public HealthCmd(PrintStream printStream) {
        this.printStream = printStream;
    }

    public HealthCmd() {
        this.printStream = System.out;
    }

    @Override
    public void execute() {
        if (helpFlag) {
            printHelpTextAsStream();
            return;
        }

        if (toolVersion){
            Package packageVersion = this.getClass().getPackage();
            String version = packageVersion.getImplementationVersion();
            printStream.println(version);
            return;
        }

        printStream.println("Please use sub command to generate artifacts." +
                "$ bal health <protocol: fhir> [OPTIONS]");

    }

    private void printHelpTextAsStream() {
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
        HealthCmdUtils.exitError(true);
    }

    @Override
    public String getName() {
        return toolName;
    }

    @Override
    public void printLongDesc(StringBuilder stringBuilder) {
        printHelpTextAsStream();

    }

    @Override
    public void printUsage(StringBuilder stringBuilder) {

    }

    @Override
    public void setParentCmdParser(CommandLine commandLine) {

    }
}
