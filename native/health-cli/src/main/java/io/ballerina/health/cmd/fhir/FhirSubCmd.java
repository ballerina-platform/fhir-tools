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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import io.ballerina.health.cmd.core.config.IgRegistryConfig;
import io.ballerina.health.cmd.core.utils.FhirIgPackageDownloader;
import io.ballerina.health.cmd.core.utils.IgModuleNameUtils;
import io.ballerina.health.cmd.core.utils.IgRegistryConfigLoader;
import io.ballerina.health.cmd.core.utils.SpecificationPathResolver;

import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.*;

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
    private List<SpecificationPathResolver.ResolvedIg> resolvedIgs = List.of();
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

    @CommandLine.Option(names = "--ig-module-name", description = "Optional module name for embedded IG resources (default: inferred from the IG). Ignored when --dependent-package is set")
    private String igModuleName;

    @CommandLine.Option(names = "--dependent-ig", description = "Dependent IG base URL and respective fully qualified Ballerina package name")
    private String[] dependentIgs;

    @CommandLine.Option(names = "--aggregate", description = "Aggregated API mode (default: true). Pass --aggregate false for separate services per resource", defaultValue = "true", fallbackValue = "true", arity = "0..1")
    private boolean aggregate = true;

    @CommandLine.Option(names = "--resources", description = "Comma-separated list of FHIR resources to include. If not specified, all available resources will be included")
    private String resources;

    @CommandLine.Option(names = "--minimal", description = "Enable minimal generation mode to skip .choreo folder, OAS files, .gitignore, and Ballerina.toml. Only generates core service files")
    private boolean minimal;

    @CommandLine.Option(names = "--flat", description = "Aggregated mode only. Generate directly into -o/--output instead of nesting under fhir-service/. Keeps Ballerina.toml, .gitignore, OAS, and .choreo (unlike --minimal). No effect with --aggregate false")
    private boolean flat;

    @CommandLine.Option(names = "--ig", description = "FHIR registry package reference as <name>[@version] (npm-style), e.g. hl7.fhir.us.core@8.0.1. Downloads from the registry when no local spec path is given. When the version is omitted, the CLI interactively lists published versions (or picks dist-tags.latest when run non-interactively). Repeatable: pass --ig more than once (template mode only) to merge profiles from multiple IGs for the same resource type into one service, dispatched by _profile -- every IG in a multi-IG run must have a known packageMappings entry")
    private String[] ig;

    @CommandLine.Option(names = "--registry-url", description = "FHIR package registry base URL (default: https://packages.fhir.org)")
    private String registryUrl;

    @CommandLine.Option(names = "--ig-cache-dir", description = "Directory to cache downloaded IG .tgz files (default: .fhir-ig-cache under the working directory)")
    private String igCacheDir;

    @CommandLine.Option(names = "--force-ig-download", description = "Re-download and re-extract the IG package even if definitions already exist locally")
    private boolean forceIgDownload;

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
        if (!CMD_CONNECTOR.equals(mode) && !CMD_MODE_TEMPLATE.equals(mode) && !CMD_MODE_PACKAGE.equals(mode)
                && (argList == null || argList.isEmpty())) {
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_NUM_OF_ARGS);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        for (String reference : igReferences()) {
            if (!FhirIgPackageDownloader.isValidIgReference(reference)) {
                printStream.println(HealthCmdConstants.PrintStrings.IG_REFERENCE_INVALID);
                printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
                HealthCmdUtils.exitError(exitWhenFinish);
                break;
            }
        }
        if (igReferences().size() > 1 && !CMD_MODE_TEMPLATE.equals(mode)) {
            printStream.println(HealthCmdConstants.PrintStrings.MULTI_IG_TEMPLATE_ONLY);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (igReferences().size() > 1 && dependentPackage != null && !dependentPackage.isEmpty()) {
            printStream.println(HealthCmdConstants.PrintStrings.MULTI_IG_DEPENDENT_PACKAGE_CONFLICT);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (CMD_MODE_PACKAGE.equals(mode) && (argList == null || argList.isEmpty())
                && !hasIg() && !canResolveSpecificationFromRegistry()) {
            printStream.println(HealthCmdConstants.PrintStrings.SPEC_PATH_REQUIRED);
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
        if (CMD_MODE_TEMPLATE.equals(mode) &&
                (dependentPackage == null || dependentPackage.isEmpty()) &&
                !hasIg() &&
                !canResolveSpecificationFromRegistry()) {
            printStream.println(HealthCmdConstants.PrintStrings.DEPENDENT_REQUIRED);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (!CMD_CONNECTOR.equals(mode) && dependentPackage != null && !dependentPackage.isEmpty()) {
            // regex matching ballerinax/health.fhir.r4
            if (!dependentPackage.matches("^(?!.*__)[a-zA-Z0-9][a-zA-Z0-9_]+[a-zA-Z0-9]/[a-zA-Z0-9][a-zA-Z0-9._]+[a-zA-Z0-9]$")) {
                printStream.println(HealthCmdConstants.PrintStrings.DEPENDENT_INCORRECT);
                printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
                HealthCmdUtils.exitError(exitWhenFinish);
            }
        }
        if (!CMD_CONNECTOR.equals(mode) && igModuleName != null && !igModuleName.isEmpty()
                && !IgModuleNameUtils.isValidBallerinaModuleName(igModuleName)) {
            printStream.println("Invalid IG module name.");
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (includedProfiles != null && excludedProfiles != null) {
            printStream.println(HealthCmdConstants.PrintStrings.INCLUDED_EXCLUDED_TOGETHER);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (CMD_MODE_CONNECTOR.equals(mode) && (configPath == null || configPath.isEmpty())) {
            //configPath is required param
            printStream.println(HealthCmdConstants.PrintStrings.INVALID_CONFIG_PATH);
            printStream.println(HealthCmdConstants.PrintStrings.HELP_FOR_MORE_INFO);
            HealthCmdUtils.exitError(exitWhenFinish);
        }
        if (this.engageSubCommand(mode, argList)) {
            if (CMD_MODE_TEMPLATE.equals(mode)) {
                printStream.println(HealthCmdConstants.PrintStrings.TEMPLATE_GEN_SUCCESS_MESSAGE + targetOutputPath);
            } else if (CMD_MODE_CONNECTOR.equals(mode)) {
                printStream.println(HealthCmdConstants.PrintStrings.CONNECTOR_GEN_SUCCESS + targetOutputPath);
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

    public boolean engageSubCommand(String mode, List<String> argList) {
        getTargetOutputPath();

        boolean explicitDependentPackage = dependentPackage != null && !dependentPackage.isEmpty();
        if (CMD_MODE_CONNECTOR.equals(mode)) {
            try {
                specificationPath = HealthCmdUtils.getSpecificationPath(configPath, executionPath.toString());
            } catch (BallerinaHealthException e) {
                printStream.println(HealthCmdConstants.PrintStrings.INVALID_CONFIG_PATH);
                throw new BLauncherException();
            }
        } else {
            try {
                String specPathArg = (argList == null || argList.isEmpty()) ? null : argList.get(argList.size() - 1);
                IgRegistryConfig registryConfig = IgRegistryConfigLoader.load();
                SpecificationPathResolver.ResolvedSpecification resolved =
                        SpecificationPathResolver.resolve(new SpecificationPathResolver.ResolveRequest(
                                mode,
                                executionPath.toString(),
                                specPathArg,
                                igReferences(),
                                registryUrl,
                                igCacheDir,
                                forceIgDownload,
                                false,
                                null,
                                registryConfig,
                                printStream
                        ));
                specificationPath = resolved.specificationPath();
                resolvedIgs = resolved.resolvedIgs();
                if (CMD_MODE_TEMPLATE.equals(mode) && !explicitDependentPackage && resolvedIgs.size() == 1
                        && resolvedIgs.get(0).mappedDependentPackage() != null) {
                    printStream.println("[INFO] IG " + resolvedIgs.get(0).igName()
                            + " maps to published package " + resolvedIgs.get(0).mappedDependentPackage()
                            + ". Embedding IG resources in the template (use --dependent-package to import that package instead).");
                }
            } catch (BallerinaHealthException e) {
                printStream.println(e.getMessage());
                printStream.println(HealthCmdConstants.PrintStrings.INVALID_SPEC_PATH);
                throw new BLauncherException();
            }
        }

        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("--package-name", packageName);
        argsMap.put("--org-name", orgName);
        argsMap.put("--package-version", packageVersion);
        argsMap.put("--included-profile", includedProfiles);
        argsMap.put("--excluded-profile", excludedProfiles);
        argsMap.put("--explicit-dependent-package", explicitDependentPackage);
        argsMap.put("--dependent-package", explicitDependentPackage ? dependentPackage : null);
        argsMap.put("--ig-module-name", igModuleName);
        argsMap.put("--dependent-ig", dependentIgs);
        argsMap.put("--aggregate", aggregate);
        argsMap.put("--resources", resources);
        argsMap.put("--minimal", minimal);
        argsMap.put("--flat", flat);
        argsMap.put("--resolved-igs", resolvedIgs);

        Handler toolHandler = null;
        try {
            toolHandler = HandlerFactory.createHandler(
                    toolName, mode, printStream, specificationPath.toString(), resolvedIgs);
        } catch (BallerinaHealthException e) {
            printStream.println(e);
            throw new BLauncherException();
        }

        toolHandler.setArgs(argsMap);
        return toolHandler.execute(specificationPath.toString(), targetOutputPath.toString());
    }

    private boolean canResolveSpecificationFromRegistry() {
        if (hasIg()) {
            return true;
        }
        if (argList != null && !argList.isEmpty()) {
            return true;
        }
        return SpecificationPathResolver.canResolveWithoutLocalSpec(
                new SpecificationPathResolver.ResolveRequest(
                        mode,
                        executionPath.toString(),
                        null,
                        igReferences(),
                        registryUrl,
                        igCacheDir,
                        forceIgDownload,
                        false,
                        null,
                        IgRegistryConfigLoader.load(),
                        printStream
                ));
    }

    /**
     * Trimmed, non-empty --ig values in the order given (possibly more than one -- see --ig's repeatable usage).
     */
    private List<String> igReferences() {
        if (ig == null || ig.length == 0) {
            return List.of();
        }
        return Arrays.stream(ig)
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .toList();
    }

    private boolean hasIg() {
        return !igReferences().isEmpty();
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
