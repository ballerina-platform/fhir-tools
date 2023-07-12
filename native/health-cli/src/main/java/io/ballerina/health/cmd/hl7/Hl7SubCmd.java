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

package io.ballerina.health.cmd.hl7;

import io.ballerina.cli.BLauncherCmd;
import picocli.CommandLine;

import java.io.PrintStream;
import java.util.List;

@CommandLine.Command(name = "hl7", description = "Generates Ballerina service/client for HL7 contract and OpenAPI contract for Ballerina service.")
public class Hl7SubCmd implements BLauncherCmd {

    private final PrintStream printStream;
    private boolean exitWhenFinish;
    private final String toolName = "hl7";
    @CommandLine.Option(names = {"--help", "-h", "?"}, usageHelp = true)
    private boolean helpFlag;

    @CommandLine.Parameters(description = "User name")
    private List<String> argList;

    public Hl7SubCmd(PrintStream printStream, boolean exitWhenFinish) {
        this.printStream = printStream;
        this.exitWhenFinish = exitWhenFinish;
    }

    public Hl7SubCmd() {
        this.printStream = System.out;
        this.exitWhenFinish = true;
    }

    @Override
    public void execute() {

        StringBuilder builder = new StringBuilder();
        builder.append("Ballerina HL7 Artifact Generator.\n\n");
        builder.append("[Note]: This is under development. Will be available soon.\n\n");
        printStream.println(builder);
    }

    @Override
    public String getName() {
        return null;
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
}
