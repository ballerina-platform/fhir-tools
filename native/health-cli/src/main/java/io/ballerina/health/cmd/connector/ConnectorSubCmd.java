package io.ballerina.health.cmd.connector;

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import io.ballerina.health.cmd.fhir.FhirSubCmd;
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

@CommandLine.Command(name = "connector", description = "Generates Ballerina connector from a FHIR Capability Statement")
public class ConnectorSubCmd implements BLauncherCmd {

    private final PrintStream printStream;
    private final boolean exitWhenFinish;
    private final String toolName = "connector";
    private final Path executionPath = Paths.get(System.getProperty("user.dir"));
    private final String resourceHome;
    private Path targetOutputPath;

    private String mode;

    public static final String CMD_MODE_CONNECTOR = "connector";
    public static final String CMD_OPTION_CONFIG_PATH = "--config";

    //resolved path from the input parameter
    private Path specificationPath;

    @CommandLine.Option(names = {"--help", "-h", "?"}, usageHelp = true, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Location of the generated Ballerina artifacts.")
    private String outputPath;

    @CommandLine.Option(names = {"-c", "--config"}, description = "Path to the tool configuration file.")
    private String configPath;

    @CommandLine.Parameters(description = "Custom arguments")
    private List<String> argList;

    public ConnectorSubCmd(PrintStream printStream, boolean exitWhenFinish) {
        this.printStream = printStream;
        this.exitWhenFinish = exitWhenFinish;
        this.resourceHome = HealthCmdUtils.getRuntimeResourcePath();
        LogManager.getLogManager().reset();
    }

    public ConnectorSubCmd() {
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
            InputStream inputStream = classLoader.getResourceAsStream(HealthCmdConstants.CMD_CONNECTOR_HELP_TEXT_FILENAME);
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

        if (configPath == null || configPath.isEmpty()) {
            //configPath is required param
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_CONFIG_PATH);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }

        mode = CMD_MODE_CONNECTOR; //only connector mode is supported for connector tool

        if (this.engageSubCommand(argList)) {
            printStream.println(HealthCmdConstants.PrintStrings.PKG_GEN_SUCCESS + targetOutputPath);
        } else {
            printStream.println(HealthCmdConstants.PrintStrings.GEN_ERROR);
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

        getTargetOutputPath();

        Path toolConfigFilePath;
        try {
            toolConfigFilePath = HealthCmdUtils.getSpecificationPath(configPath, executionPath.toString());
        } catch (BallerinaHealthException e) {
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_CONFIG_PATH);
            throw new BLauncherException();
        }

        Handler toolHandler;
        try {
            toolHandler = HandlerFactory.createHandler(toolName, mode, printStream, toolConfigFilePath.toString());
        } catch (BallerinaHealthException e) {
            printStream.println(e);
            throw new BLauncherException();
        }
        return toolHandler.execute(toolConfigFilePath.toString(), targetOutputPath.toString());
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
//            targetOutputPath = Paths.get(targetOutputPath + File.separator + "generated-connector");

        }
    }
}
