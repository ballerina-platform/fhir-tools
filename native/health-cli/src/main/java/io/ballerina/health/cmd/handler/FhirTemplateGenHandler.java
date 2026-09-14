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

package io.ballerina.health.cmd.handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.health.cmd.core.config.HealthCmdConfig;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.ErrorMessages;
import io.ballerina.health.cmd.core.utils.FhirIgPackageDownloader;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import io.ballerina.health.cmd.core.utils.IgModuleNameUtils;
import io.ballerina.health.cmd.core.utils.SpecificationPathResolver;
import io.ballerina.health.cmd.core.utils.SpecificationPathUtils;
import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.Tool;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRSpecParser;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.FHIRSpecParserFactory;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.FHIRTool;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.config.FHIRToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.BallerinaPackageGenTool;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Handler for template generation.
 */
public class FhirTemplateGenHandler implements Handler {

    private String packageName;
    private String orgName;
    private String packageVersion;
    private String fhirVersion;
    private String dependentPackage;
    private boolean generateIgModule;
    private String igModuleName;
    private List<SpecificationPathResolver.ResolvedIg> resolvedIgs;

    private String[] includedProfiles;
    private String[] excludedProfiles;
    private boolean aggregate;
    private String resources;
    private boolean minimal;
    private boolean flat;

    private JsonObject configJson;
    private PrintStream printStream;

    private FHIRTool fhirToolLib;

    @Override
    public void init(PrintStream printStream, String specificationPath) {
        init(printStream, specificationPath, List.of());
    }

    @Override
    public void init(PrintStream printStream, String specificationPath,
                     List<SpecificationPathResolver.ResolvedIg> resolvedIgs) {

        this.printStream = printStream;
        this.resolvedIgs = resolvedIgs;
        try {
            configJson = HealthCmdConfig.getParsedConfigFromStream(HealthCmdUtils.getResourceFile(
                    this.getClass(), HealthCmdConstants.CMD_CONFIG_FILENAME));
        } catch (BallerinaHealthException e) {
            throw new RuntimeException(e);
        }
        if (isMultiIg()) {
            fhirToolLib = initializeMultiIgLib(printStream, specificationPath, resolvedIgs);
        } else {
            fhirToolLib = (FHIRTool) initializeLib(
                    HealthCmdConstants.CMD_SUB_FHIR, printStream, configJson, specificationPath);
        }
        fhirVersion = fhirToolLib.getFhirVersion();
    }

    /**
     * Multi-IG equivalent of {@link Handler#initializeLib}: that default only ever calls
     * {@code specParser.parseIG(fhirToolConfig, "FHIR", specificationPath)} once, treating the whole given path
     * as one IG's own directory (no subfolder discovery happens anywhere in the framework -- confirmed by reading
     * its source). For multi-IG, parseIG needs to be called once per resolved IG, each with its own real name and
     * its own spec/&lt;name&gt; leaf directory (the framework maps FHIRImplementationGuide by whatever name it's
     * called with, independent of file content), so that each profile parsed keeps its own IG identity for the
     * per-IG package resolution in FHIRProfile.setPackagePrefix().
     */
    private FHIRTool initializeMultiIgLib(PrintStream printStream, String specBasePath,
                                          List<SpecificationPathResolver.ResolvedIg> resolvedIgs) {
        try {
            String detectedFhirVersion = HealthCmdUtils.getSpecFhirVersion(specBasePath);
            if (detectedFhirVersion == null) {
                printStream.println(ErrorMessages.LIB_INITIALIZING_FAILED
                        + "Unable to find FHIR version in the specification");
                return null;
            }
            FHIRToolConfig fhirToolConfig = new FHIRToolConfig();
            fhirToolConfig.configure(new JsonConfigType(configJson));
            fhirToolConfig.setSpecBasePath(specBasePath);
            FHIRTool multiIgToolLib = new FHIRTool(detectedFhirVersion);
            multiIgToolLib.initialize(fhirToolConfig);

            AbstractFHIRSpecParser specParser = FHIRSpecParserFactory.getParser(detectedFhirVersion);
            for (SpecificationPathResolver.ResolvedIg resolvedIg : resolvedIgs) {
                String igDir = Paths.get(specBasePath,
                        FhirIgPackageDownloader.sanitizeIgDirectoryName(resolvedIg.igName())).toString();
                specParser.parseIG(fhirToolConfig, resolvedIg.igName(), igDir);
            }
            return multiIgToolLib;
        } catch (IOException e) {
            printStream.println(ErrorMessages.FHIR_VERSION_READ_ERROR + e.getMessage());
            return null;
        } catch (CodeGenException e) {
            printStream.println(ErrorMessages.LIB_INITIALIZING_FAILED + e.getMessage());
            HealthCmdUtils.throwLauncherException(e);
            return null;
        }
    }

    @Override
    public void setArgs(Map<String, Object> argsMap) {

        this.packageName = (String) argsMap.get("--package-name");
        this.orgName = (String) argsMap.get("--org-name");
        this.packageVersion = (String) argsMap.get("--package-version");
        this.dependentPackage = (String) argsMap.get("--dependent-package");
        boolean explicitDependentPackage = Boolean.TRUE.equals(argsMap.get("--explicit-dependent-package"));
        this.generateIgModule = !explicitDependentPackage;
        this.igModuleName = (String) argsMap.get("--ig-module-name");
        this.includedProfiles = (String[]) argsMap.get("--included-profile");
        this.excludedProfiles = (String[]) argsMap.get("--excluded-profile");
        Object aggregateArg = argsMap.get("--aggregate");
        this.aggregate = aggregateArg instanceof Boolean ? (Boolean) aggregateArg : true;
        this.minimal = (Boolean) argsMap.get("--minimal");
        this.flat = Boolean.TRUE.equals(argsMap.get("--flat"));
        this.resources = (String) argsMap.get("--resources");
        @SuppressWarnings("unchecked")
        List<SpecificationPathResolver.ResolvedIg> resolved =
                (List<SpecificationPathResolver.ResolvedIg>) argsMap.get("--resolved-igs");
        this.resolvedIgs = resolved;
    }

    private boolean isMultiIg() {
        return resolvedIgs != null && resolvedIgs.size() > 1;
    }

    @Override
    public boolean execute(String specificationPath, String targetOutputPath) {
        if (!isMultiIg()) {
            // The international-base-default detection below reads the whole specification path looking for
            // one, tool-wide dependent package -- meaningless once every IG already has its own resolved
            // package (see the multi-IG branch in the igConfig block further down).
            try {
                applyInternationalBaseDefaults(specificationPath);
            } catch (IOException e) {
                throw new RuntimeException("Unable to evaluate specification path for template generation", e);
            }
        }

        JsonElement toolExecConfigs = null;
        if (configJson != null) {
            toolExecConfigs = configJson.getAsJsonObject("fhir").getAsJsonObject("tools").getAsJsonObject(HealthCmdConstants.CMD_MODE_TEMPLATE);
        } else {
            printStream.println(ErrorMessages.CONFIG_PARSE_ERROR);
            HealthCmdUtils.exitError(true);
        }

        if (toolExecConfigs != null) {
            JsonObject toolExecConfig = toolExecConfigs.getAsJsonObject();

            //override tool level configs here
            Tool tool;
            TemplateGenerator mainTemplateGenerator = null;

            try {
                ClassLoader classLoader = this.getClass().getClassLoader();
                String configClassName = "org.wso2.healthcare.fhir.codegen.ballerina.project.tool." +
                        "config.BallerinaProjectToolConfig";
                Class<?> configClass = classLoader.loadClass(configClassName);

                String toolClassName = "org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectToolFactory";
                Class<?> toolClass = classLoader.loadClass(toolClassName);

                ToolConfig toolConfigInstance = (ToolConfig) configClass.getConstructor().newInstance();
                toolConfigInstance.setTargetDir(targetOutputPath);
                toolConfigInstance.setToolName(HealthCmdConstants.CMD_MODE_TEMPLATE);

                toolConfigInstance.configure(new JsonConfigType(
                        toolExecConfig.getAsJsonObject().getAsJsonObject("config")));

                //override default configs for package-gen mode with user provided configs
                if (orgName != null && !orgName.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(orgName.toLowerCase());
                    toolConfigInstance.overrideConfig("project.package.org", overrideConfig);
                }
                if (packageVersion != null && !packageVersion.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(packageVersion.toLowerCase());
                    toolConfigInstance.overrideConfig("project.package.version", overrideConfig);
                }
                if (fhirVersion != null && !fhirVersion.isEmpty() && !fhirVersion.equalsIgnoreCase("r4")) {
                    JsonElement overrideConfig = new Gson().toJsonTree(fhirVersion.toLowerCase());
                    toolConfigInstance.overrideConfig("project.fhir.default_version", overrideConfig);
                }
                if (packageName != null && !packageName.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(packageName.toLowerCase());
                    toolConfigInstance.overrideConfig("project.package.templateName", overrideConfig);
                }
                if (isMultiIg()) {
                    // Every IG in a multi-IG run already resolved to a known published package (enforced in
                    // SpecificationPathResolver) -- never embed, and give each IG its own igConfig entry keyed
                    // by its real name instead of the single "FHIR" constant used below, so the profiles the
                    // framework discovers per real IG name (see AbstractBallerinaProjectTool.populateIGs())
                    // line up with a matching config entry.
                    toolConfigInstance.overrideConfig("project.generateIgModule.enabled", new Gson().toJsonTree(false));
                    for (SpecificationPathResolver.ResolvedIg resolvedIg : resolvedIgs) {
                        toolConfigInstance.overrideConfig("project.package.igConfig", populateIGConfig(
                                resolvedIg.igName(),
                                orgName,
                                resolvedIg.mappedDependentPackage(),
                                includedProfiles,
                                excludedProfiles
                        ));
                    }
                } else {
                    if (generateIgModule) {
                        String resolvedIgModuleName = resolveIgModuleName(specificationPath);
                        String igModuleSourcePath = generateIgModuleSource(specificationPath, targetOutputPath,
                                toolExecConfig);
                        JsonElement generateIgModuleEnabled = new Gson().toJsonTree(true);
                        JsonElement generateIgModuleNameConfig = new Gson().toJsonTree(resolvedIgModuleName);
                        JsonElement generateIgModuleSourceDir = new Gson().toJsonTree(igModuleSourcePath);
                        toolConfigInstance.overrideConfig("project.generateIgModule.enabled", generateIgModuleEnabled);
                        toolConfigInstance.overrideConfig("project.generateIgModule.name", generateIgModuleNameConfig);
                        toolConfigInstance.overrideConfig("project.generateIgModule.sourceDir", generateIgModuleSourceDir);
                    } else {
                        toolConfigInstance.overrideConfig("project.generateIgModule.enabled", new Gson().toJsonTree(false));
                        if (dependentPackage != null && !dependentPackage.isEmpty()) {
                            JsonElement overrideConfig = new Gson().toJsonTree(dependentPackage);
                            JsonElement nameConfig = new Gson().toJsonTree(
                                    dependentPackage.substring(dependentPackage.lastIndexOf('/') + 1));
                            toolConfigInstance.overrideConfig("project.package.dependentPackage", overrideConfig);
                            toolConfigInstance.overrideConfig("project.package.namePrefix", nameConfig);
                        }
                    }
                    toolConfigInstance.overrideConfig("project.package.igConfig", populateIGConfig(
                                    HealthCmdConstants.CMD_DEFAULT_IG_NAME,
                                    orgName,
                                    getEffectiveDependentPackage(),
                                    includedProfiles,
                                    excludedProfiles
                            )
                    );
                }

                // Configure aggregated API settings (default: enabled)
                toolConfigInstance.overrideConfig("project.enableAggregatedApi", new Gson().toJsonTree(aggregate));
                if (aggregate && resources != null && !resources.trim().isEmpty()) {
                    String[] resourceArray = resources.split(",");
                    JsonArray resourcesArray = new JsonArray();
                    for (String resource : resourceArray) {
                        resourcesArray.add(resource.trim());
                    }
                    toolConfigInstance.overrideConfig("project.aggregatedApis", resourcesArray);
                }
                // Configure minimal generation settings
                if (minimal) {
                    JsonElement minimalConfig = new Gson().toJsonTree(true);
                    toolConfigInstance.overrideConfig("project.minimalGeneration", minimalConfig);
                }
                if (flat) {
                    toolConfigInstance.overrideConfig("project.flatOutput", new Gson().toJsonTree(true));
                }

                Object toolFactory = toolClass.getConstructor().newInstance();
                Method getToolMethod = toolClass.getMethod("getBallerinaProjectTool", String.class);
                tool = (Tool) getToolMethod.invoke(toolFactory, fhirVersion);

                tool.initialize(toolConfigInstance);
                fhirToolLib.getToolImplementations().putIfAbsent(HealthCmdConstants.CMD_MODE_PACKAGE, tool);
                mainTemplateGenerator = tool.execute(fhirToolLib.getToolContext());

            } catch (ClassNotFoundException e) {
                printStream.println(ErrorMessages.TOOL_IMPL_NOT_FOUND + e.getMessage());
                HealthCmdUtils.throwLauncherException(e);

            } catch (InstantiationException | IllegalAccessException e) {
                printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
                HealthCmdUtils.throwLauncherException(e);

            } catch (CodeGenException e) {
                printStream.println(ErrorMessages.UNKNOWN_ERROR);
                printStream.println(e);
                HealthCmdUtils.throwLauncherException(e);

            } catch (InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            if (mainTemplateGenerator != null) {
                try {
                    mainTemplateGenerator.generate(
                            fhirToolLib.getToolContext(),
                            mainTemplateGenerator.getGeneratorProperties());
                    TemplateGenerator childTemplateGenerator = mainTemplateGenerator.getChildTemplateGenerator();
                    HealthCmdUtils.engageChildTemplateGenerators(
                            childTemplateGenerator,
                            fhirToolLib.getToolContext(),
                            mainTemplateGenerator.getGeneratorProperties());
                } catch (CodeGenException e) {
                    printStream.println(ErrorMessages.UNKNOWN_ERROR + e.getMessage());
                    HealthCmdUtils.throwLauncherException(e);
                }
                return true;
            } else {
                printStream.println("Template generator is not registered for the tool: " + HealthCmdConstants.CMD_MODE_PACKAGE);
                printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
            }
        }
        return false;
    }

    private String resolveIgModuleName(String specificationPath) {
        if (igModuleName != null && !igModuleName.isEmpty()) {
            return igModuleName;
        }
        try {
            return IgModuleNameUtils.inferIgModuleName(specificationPath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to infer IG module name from specification", e);
        }
    }

    private String generateIgModuleSource(String specificationPath, String targetOutputPath,
                                          JsonObject toolExecConfig) {
        try {
            BallerinaPackageGenToolConfig packageToolConfig = new BallerinaPackageGenToolConfig();
            String generatedIgPackagePath = Paths.get(targetOutputPath, ".generated-ig-package").toString();
            Path generatedIgPackagePathObj = Paths.get(generatedIgPackagePath);
            if (Files.exists(generatedIgPackagePathObj)) {
                deleteRecursively(generatedIgPackagePathObj.toFile());
            }
            packageToolConfig.setTargetDir(generatedIgPackagePath);
            packageToolConfig.setToolName(HealthCmdConstants.CMD_MODE_PACKAGE);
            JsonObject packageExecConfig = configJson.getAsJsonObject("fhir").getAsJsonObject("tools")
                    .getAsJsonObject(HealthCmdConstants.CMD_MODE_PACKAGE).getAsJsonObject("config");
            packageToolConfig.configure(new JsonConfigType(packageExecConfig));

            String packageNameToGenerate = getPackageNameForIgModule(packageExecConfig);
            packageToolConfig.overrideConfig("packageConfig.name", new Gson().toJsonTree(packageNameToGenerate));
            if (orgName != null && !orgName.isEmpty()) {
                packageToolConfig.overrideConfig("packageConfig.org", new Gson().toJsonTree(orgName.toLowerCase()));
            }
            if (packageVersion != null && !packageVersion.isEmpty()) {
                packageToolConfig.overrideConfig("packageConfig.packageVersion",
                        new Gson().toJsonTree(packageVersion.toLowerCase()));
            }
            if (fhirVersion != null && !fhirVersion.isEmpty()) {
                packageToolConfig.overrideConfig("packageConfig.fhirVersion", new Gson().toJsonTree(fhirVersion));
            }

            Tool packageTool = new BallerinaPackageGenTool();
            packageTool.initialize(packageToolConfig);
            TemplateGenerator packageTemplateGenerator = packageTool.execute(fhirToolLib.getToolContext());
            if (packageTemplateGenerator != null) {
                packageTemplateGenerator.generate(
                        fhirToolLib.getToolContext(), packageTemplateGenerator.getGeneratorProperties());
                HealthCmdUtils.engageChildTemplateGenerators(
                        packageTemplateGenerator.getChildTemplateGenerator(),
                        fhirToolLib.getToolContext(), packageTemplateGenerator.getGeneratorProperties());
            }
            return Paths.get(generatedIgPackagePath, packageNameToGenerate).toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating IG module source package", e);
        }
    }

    private String getPackageNameForIgModule(JsonObject packageExecConfigObj) {
        if (packageName != null && !packageName.isEmpty()) {
            return packageName;
        }
        if (dependentPackage != null && dependentPackage.contains("/")) {
            return dependentPackage.substring(dependentPackage.lastIndexOf('/') + 1);
        }
        return packageExecConfigObj.getAsJsonObject("packageConfigs").getAsJsonPrimitive("name").getAsString();
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to delete file: " + file.getAbsolutePath());
        }
    }

    private void applyInternationalBaseDefaults(String specificationPath) throws IOException {
        boolean useInternationalBase = SpecificationPathUtils.isInternationalBaseSpecification(specificationPath);
        if ((dependentPackage == null || dependentPackage.isEmpty()) && useInternationalBase) {
            dependentPackage = getDefaultDependentPackageForFhirVersion();
            printStream.println(HealthCmdConstants.PrintStrings.USING_INTERNATIONAL_BASE + dependentPackage + ".");
        }
        if (generateIgModule && useInternationalBase) {
            generateIgModule = false;
            printStream.println("[INFO] International base structure definitions detected. "
                    + "Using published dependent package " + dependentPackage + " instead of embedding an IG module.");
        }
    }

    private String getDefaultDependentPackageForFhirVersion() {
        if (fhirVersion != null && fhirVersion.equalsIgnoreCase("r5")) {
            return HealthCmdConstants.CMD_DEFAULT_R5_DEPENDENT_PACKAGE;
        }
        return HealthCmdConstants.CMD_DEFAULT_R4_DEPENDENT_PACKAGE;
    }

    private String getEffectiveDependentPackage() {
        if (dependentPackage != null && !dependentPackage.isEmpty()) {
            return dependentPackage;
        }
        return getDefaultDependentPackageForFhirVersion();
    }

    private JsonObject populateIGConfig(String name, String orgName, String dependentPackageName,
                                        String[] includedProfiles, String[] excludedProfiles) {

        JsonObject igConfig = new JsonObject();
        igConfig.addProperty("implementationGuide", name);
        String importStatement = dependentPackageName;
        if (orgName != null && !orgName.isEmpty() && !dependentPackageName.contains("/")) {
            importStatement = orgName + "/" + dependentPackageName;
        }
        igConfig.addProperty("importStatement", importStatement);
        igConfig.addProperty("enable", true);
        JsonArray includedProfilesArray = new JsonArray();

        if (includedProfiles != null) {
            for (String profile : includedProfiles) {
                includedProfilesArray.add(profile);
            }
        }
        igConfig.add("includedProfiles", includedProfilesArray);
        JsonArray excludedProfilesArray = new JsonArray();
        if (excludedProfiles != null) {
            for (String profile : excludedProfiles) {
                excludedProfilesArray.add(profile);
            }
        }
        igConfig.add("excludedProfiles", excludedProfilesArray);
        return igConfig;
    }
}
