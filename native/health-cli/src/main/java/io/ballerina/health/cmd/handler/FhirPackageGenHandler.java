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

import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.*;

/**
 * Handler for package generation tool.
 */
public class FhirPackageGenHandler implements Handler {

    private String packageName;
    private String orgName;
    private String packageVersion;
    private String fhirVersion;
    private String[] dependentIgs;

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
        fhirToolLib = (FHIRTool) initializeLib(HealthCmdConstants.CMD_SUB_FHIR, printStream, configJson, specificationPath);
    }

    @Override
    public void setArgs(Map<String, Object> argsMap) {

        this.packageName = (String) argsMap.get("--package-name");
        this.orgName = (String) argsMap.get("--org-name");
        this.packageVersion = (String) argsMap.get("--package-version");
        this.fhirVersion = (String) argsMap.get("--fhir-version");
        this.dependentIgs = (String[]) argsMap.get("--dependent-ig");
    }

    /**
     * @param specificationPath The directory which contains the profile definitions like resources, datatypes, code systems etc
     * @param targetOutputPath  The directory which will contain the generated Ballerina package once the tool got executed successfully
     * @return true if tool got executed successfully
     */
    @Override
    public boolean execute(String specificationPath, String targetOutputPath) {

        // Holds the default configs
        JsonElement toolExecConfigs = null;
        if (configJson != null) {
            // Read default values from resource/tool-config.json -> fhir.tools.package
            toolExecConfigs = configJson.getAsJsonObject("fhir").getAsJsonObject("tools").getAsJsonObject(HealthCmdConstants.CMD_MODE_PACKAGE);
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
                String configClassName = "org.wso2.healthcare.fhir.ballerina.packagegen.tool.config." +
                        "BallerinaPackageGenToolConfig";
                Class<?> configClazz = classLoader.loadClass(configClassName);

                ToolConfig toolConfigInstance = (ToolConfig) configClazz.getConstructor().newInstance();
                toolConfigInstance.setTargetDir(targetOutputPath);
                toolConfigInstance.setToolName(HealthCmdConstants.CMD_MODE_PACKAGE);

                // Feed default project configs to ToolConfig instance
                toolConfigInstance.configure(new JsonConfigType(toolExecConfig.getAsJsonObject().getAsJsonObject("config")));

                //override default configs for package-gen mode with user provided configs
                if (packageName != null && !packageName.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(packageName.toLowerCase());
                    toolConfigInstance.overrideConfig("packageConfig.name", overrideConfig);
                }
                if (orgName != null && !orgName.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(orgName.toLowerCase());
                    toolConfigInstance.overrideConfig("packageConfig.org", overrideConfig);
                }
                if (packageVersion != null && !packageVersion.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(packageVersion.toLowerCase());
                    toolConfigInstance.overrideConfig("packageConfig.packageVersion", overrideConfig);
                }
                if(fhirVersion != null && !fhirVersion.isEmpty()){
                    JsonElement overrideConfig = new Gson().toJsonTree(fhirVersion.toLowerCase());
                    toolConfigInstance.overrideConfig("packageConfig.fhirVersion", overrideConfig);
                }
                if (fhirVersion != null && !fhirVersion.isEmpty() && fhirVersion.equalsIgnoreCase("r5")){
                    // Override basePackage and dependentPackage in tool-config.json
                    final String r5Repository = "https://github.com/ballerina-platform/module-ballerinax-health.fhir.r5";
                    final String r5BasePackage = "ballerinax/health.fhir.r5";

                    JsonElement overrideConfigRepository = new Gson().toJsonTree(r5Repository);
                    JsonElement overrideConfigBase = new Gson().toJsonTree(r5BasePackage);

                    toolConfigInstance.overrideConfig("packageConfig.repository", overrideConfigRepository);
                    toolConfigInstance.overrideConfig("packageConfig.basePackage", overrideConfigBase);
                }
                if (dependentIgs != null && dependentIgs.length > 0) {
                    // Create a JSON array from the String array
                    JsonArray dependentIgJsonArray = new JsonArray();
                    for (String dependentIg : dependentIgs) {
                        dependentIgJsonArray.add(dependentIg);
                    }
                    JsonElement overrideConfig = new Gson().toJsonTree(dependentIgJsonArray);
                    toolConfigInstance.overrideConfig("packageConfig.dependent.igs", overrideConfig);
                }

                String toolClassName = "org.wso2.healthcare.fhir.ballerina.packagegen.tool.BallerinaPackageGenTool";
                Class<?> toolClazz = classLoader.loadClass(toolClassName);
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
}
