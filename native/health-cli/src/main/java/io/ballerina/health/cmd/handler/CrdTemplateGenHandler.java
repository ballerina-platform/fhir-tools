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

package io.ballerina.health.cmd.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
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

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.*;
import static io.ballerina.health.cmd.core.utils.HealthCmdConstants.PrintStrings.CDS_HOOKS_VALIDATION;
import static io.ballerina.health.cmd.core.utils.HealthCmdUtils.exitError;
import static io.ballerina.health.cmd.core.utils.HealthCmdUtils.parseTomlToJson;

/**
 * Handler for template generation.
 */
public class CrdTemplateGenHandler implements Handler {

    private String packageName;
    private String orgName;
    private String packageVersion;
    private String fhirVersion;
    private JsonObject configJson;
    private InputStream cdsHooksJsonSchemaStream;
    private PrintStream printStream;

    @Override
    public void init(PrintStream printStream, String cdsToolConfigFilePath) {

        this.printStream = printStream;
        try {
            configJson = HealthCmdConfig.getParsedConfigFromStream(HealthCmdUtils.getResourceFile(
                    this.getClass(), HealthCmdConstants.CMD_CDS_CONFIG_FILENAME));

            cdsHooksJsonSchemaStream = HealthCmdUtils.getResourceFile(this.getClass(), CMD_CDS_JSON_SCHEMA_FILENAME);
        } catch (BallerinaHealthException e) {
            HealthCmdUtils.throwLauncherException(e);
        }
    }

    @Override
    public void setArgs(Map<String, Object> argsMap) {
        this.packageName = (String) argsMap.get(CMD_OPTION_PACKAGE_NAME);
        this.orgName = (String) argsMap.get(CMD_OPTION_ORG_NAME);
        this.packageVersion = (String) argsMap.get(CMD_OPTION_PACKAGE_VERSION);
        this.fhirVersion = (String) argsMap.get(HealthCmdConstants.CMD_OPTION_FHIR_VERSION);
    }

    @Override
    public boolean execute(String cdsHookDefinitionFilePath, String targetOutputPath) {

        JsonElement toolExecConfigs = null;
        if (configJson != null) {
            toolExecConfigs = configJson.getAsJsonObject(CDS).getAsJsonObject(TOOLS).getAsJsonObject(HealthCmdConstants.CMD_MODE_TEMPLATE);
        } else {
            printStream.println(ErrorMessages.CONFIG_PARSE_ERROR);
            exitError(true);
        }

        if (toolExecConfigs != null) {
            JsonElement cdsHooksJson = parseTomlToJson(cdsHookDefinitionFilePath);

            // This method will validate the cds hooks json with the json schema
            validateCdsHooks(cdsHooksJson);

            JsonObject toolExecConfigsAsJsonObject = toolExecConfigs.getAsJsonObject();
            toolExecConfigsAsJsonObject.add(HOOKS, cdsHooksJson);
            toolExecConfigs = toolExecConfigsAsJsonObject;

            Tool tool;
            TemplateGenerator crdTemplateGenerator = null;
            try {
                ClassLoader classLoader = this.getClass().getClassLoader();
                Class<?> configClazz = classLoader.loadClass(CDS_CONFIG_CLASS_NAME);
                ToolConfig toolConfigInstance = (ToolConfig) configClazz.getConstructor().newInstance();
                toolConfigInstance.setTargetDir(targetOutputPath);
                toolConfigInstance.setToolName(HealthCmdConstants.CMD_MODE_TEMPLATE);

                toolConfigInstance.configure(new JsonConfigType(
                        toolExecConfigs.getAsJsonObject()));

                //override default configs for package-gen mode with user provided configs
                if (orgName != null && !orgName.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(orgName.toLowerCase());
                    toolConfigInstance.overrideConfig(PROJECT_PACKAGE_ORG, overrideConfig);
                }
                if (packageVersion != null && !packageVersion.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(packageVersion.toLowerCase());
                    toolConfigInstance.overrideConfig(PROJECT_PACKAGE_VERSION, overrideConfig);
                }
                if (packageName != null && !packageName.isEmpty()) {
                    JsonElement overrideConfig = new Gson().toJsonTree(packageName.toLowerCase());
                    toolConfigInstance.overrideConfig(PROJECT_PACKAGE_NAME_PREFIX, overrideConfig);
                }
                if (fhirVersion != null && !fhirVersion.isEmpty() && fhirVersion.equalsIgnoreCase("r5")) {
                    // Override basePackage and dependentPackage in cds-tool-config.json
                    final String r5BasePackage = "ballerinax/health.fhir.r5";
                    final String r5DependentPackage = "ballerinax/health.fhir.r5.cds"; // MIGHT NOT BE AVAILABLE

                    JsonElement overrideConfigBase = new Gson().toJsonTree(r5BasePackage);
                    JsonElement overrideConfigDependent = new Gson().toJsonTree(r5DependentPackage);

                    toolConfigInstance.overrideConfig(PROJECT_PACKAGE_BASE_PACKAGE, overrideConfigBase);
                    toolConfigInstance.overrideConfig(PROJECT_PACKAGE_DEPENDENT_PACKAGE, overrideConfigDependent);
                }

                Class<?> toolClazz = classLoader.loadClass(CDS_TOOL_CLASS_NAME);
                tool = (Tool) toolClazz.getConstructor().newInstance();
                tool.initialize(toolConfigInstance);

                // Here don't have a ToolContext context object to pass
                // and it is related FHIR implementation
                crdTemplateGenerator = tool.execute(null);
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
                HealthCmdUtils.throwLauncherException(e);
            }

            if (crdTemplateGenerator != null) {
                try {
                    crdTemplateGenerator.generate(null, crdTemplateGenerator.getGeneratorProperties());
                } catch (CodeGenException e) {
                    printStream.println(ErrorMessages.UNKNOWN_ERROR + e.getMessage());
                    HealthCmdUtils.throwLauncherException(e);
                }
                return true;
            } else {
                printStream.println("Template generator is not registered for the tool: " + HealthCmdConstants.CMD_MODE_TEMPLATE);
                printStream.println(ErrorMessages.CONFIG_INITIALIZING_FAILED);
            }
        }
        return false;
    }


    // This method will validate the cds hooks json with the json schema
    // If there are any validation errors, it will print all and exit
    private void validateCdsHooks(JsonElement cdsHooksJson) {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema jsonSchema = factory.getSchema(cdsHooksJsonSchemaStream);
        JsonNode jsonNode = null;
        try {
            jsonNode = mapper.readTree(cdsHooksJson.toString());
        } catch (JsonProcessingException e) {
            HealthCmdUtils.throwLauncherException(e);
        }
        Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);

        if (!errors.isEmpty()) {
            printStream.println(CDS_HOOKS_VALIDATION);
        }

        for (ValidationMessage error : errors) {
            printStream.println(error.getMessage());
        }
        if (!errors.isEmpty()) {
            exitError(true);
        }
    }
}
