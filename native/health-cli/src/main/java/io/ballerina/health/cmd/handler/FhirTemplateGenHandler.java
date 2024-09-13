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
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.Tool;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.FHIRTool;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Handler for template generation.
 */
public class FhirTemplateGenHandler implements Handler {

    private String packageName;
    private String orgName;
    private String version;
    private String dependentPackage;

    private String[] includedProfiles;
    private String[] excludedProfiles;

    private JsonObject configJson;
    private PrintStream printStream;

    private FHIRTool fhirToolLib;

    @Override
    public void init(PrintStream printStream, String specificationPath) {

        this.printStream = printStream;
        try {
            configJson = HealthCmdConfig.getParsedConfigFromStream(HealthCmdUtils.getResourceFile(
                    this.getClass(), HealthCmdConstants.CMD_CONFIG_FILENAME));
        } catch (BallerinaHealthException e) {
            throw new RuntimeException(e);
        }
        fhirToolLib = (FHIRTool) initializeLib(
                HealthCmdConstants.CMD_SUB_FHIR, printStream, configJson, specificationPath);
    }

    @Override
    public void setArgs(Map<String, Object> argsMap) {

        this.packageName = (String) argsMap.get("--package-name");
        this.orgName = (String) argsMap.get("--org-name");
        this.version = (String) argsMap.get("--package-version");
        this.dependentPackage = (String) argsMap.get("--dependent-package");
        this.includedProfiles = (String[]) argsMap.get("--included-profile");
        this.excludedProfiles = (String[]) argsMap.get("--excluded-profile");
    }

    @Override
    public boolean execute(String specificationPath, String targetOutputPath) {

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
                Class<?> configClazz = classLoader.loadClass(configClassName);
                String toolClassName = "org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectTool";
                Class<?> toolClazz = classLoader.loadClass(toolClassName);
                ToolConfig toolConfigInstance = (ToolConfig) configClazz.getConstructor().newInstance();
                toolConfigInstance.setTargetDir(targetOutputPath);
                toolConfigInstance.setToolName(HealthCmdConstants.CMD_MODE_TEMPLATE);

                toolConfigInstance.configure(new JsonConfigType(
                        toolExecConfig.getAsJsonObject().getAsJsonObject("config")));

                //override default configs for package-gen mode with user provided configs
                if (orgName != null && !orgName.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(orgName.toLowerCase());
                    toolConfigInstance.overrideConfig("project.package.org", overrideConfig);
                }
                if (version != null && !version.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(version.toLowerCase());
                    toolConfigInstance.overrideConfig("project.package.version", overrideConfig);
                }
                if (dependentPackage != null) {
                    JsonElement overrideConfig = new Gson().toJsonTree(dependentPackage);
                    JsonElement nameConfig =
                            new Gson().toJsonTree(dependentPackage.substring(dependentPackage.lastIndexOf('/') + 1));
                    toolConfigInstance.overrideConfig("project.package.dependentPackage", overrideConfig);
                    toolConfigInstance.overrideConfig("project.package.namePrefix", nameConfig);
                }
                toolConfigInstance.overrideConfig("project.package.igConfig", populateIGConfig(
                                HealthCmdConstants.CMD_DEFAULT_IG_NAME,
                                orgName,
                                includedProfiles,
                                excludedProfiles
                        )
                );

                tool = (Tool) toolClazz.getConstructor().newInstance();
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

    private JsonObject populateIGConfig(String name, String orgName, String[] includedProfiles,
                                        String[] excludedProfiles) {

        JsonObject igConfig = new JsonObject();
        igConfig.addProperty("implementationGuide", name);
        String importStatement = orgName != null ? orgName : HealthCmdConstants.CMD_DEFAULT_ORG_NAME + "/" + name;
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
