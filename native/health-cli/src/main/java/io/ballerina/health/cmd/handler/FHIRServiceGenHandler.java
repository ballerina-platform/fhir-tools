/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.AbstractToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.Tool;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.FHIRSpecParser;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.FHIRTool;

import java.io.Console;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tool execution handler class for EHR service gen tool.
 */
public class FHIRServiceGenHandler implements Handler {
    private JsonObject configJson;
    private PrintStream printStream;
    private FHIRTool fhirToolLib;
    private String projectName;
    private String fhirServerName;
    private String orgName;
    private String[] includedProfiles;
    private String dependentPackage;
    //capability statement file path or url
    private String capabilityStatementPath;
    //SMART configuration file path or url
    private String smartConfigPath;
    private String authMethod;


    public FHIRServiceGenHandler(PrintStream printStream, String specificationPath) {
        this.init(printStream, specificationPath);
    }

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
        for (String key : argsMap.keySet()) {
            switch (key) {
                case HealthCmdConstants.CommandOptions.CMD_OPT_PROJECT_NAME ->
                        this.projectName = (String) argsMap.get(key);
                case HealthCmdConstants.CommandOptions.CMD_OPT_SERVER_NAME ->
                        this.fhirServerName = (String) argsMap.get(key);
                case HealthCmdConstants.CommandOptions.CMD_OPT_ORG_NAME -> this.orgName = (String) argsMap.get(key);
                case HealthCmdConstants.CommandOptions.CMD_OPT_INCLUDED_PROFILE ->
                        this.includedProfiles = (String[]) argsMap.get(key);
                case HealthCmdConstants.CommandOptions.CMD_DEPENDENT_PACKAGE ->
                        this.dependentPackage = (String) argsMap.get(key);
                case HealthCmdConstants.CommandOptions.CMD_OPT_CAPABILITY_STATEMENT ->
                        this.capabilityStatementPath = (String) argsMap.get(key);
                case HealthCmdConstants.CommandOptions.CMD_OPT_SMART_CONFIGURATION ->
                        this.smartConfigPath = (String) argsMap.get(key);
                default -> {
                    printStream.println(ErrorMessages.INVALID_OPTION_PROVIDED + key);
                    HealthCmdUtils.exitError(true);
                }
            }
        }
    }

    @Override
    public boolean execute(String specificationPath, String targetOutputPath) {
        JsonElement templateToolExecConfigs;
        JsonElement serviceToolExecConfigs;
        if (configJson == null) {
            printStream.println(ErrorMessages.CONFIG_PARSE_ERROR);
            HealthCmdUtils.exitError(true);
        }
        templateToolExecConfigs = configJson.getAsJsonObject("fhir")
                .getAsJsonObject("tools").getAsJsonObject(HealthCmdConstants.CMD_MODE_TEMPLATE);
        serviceToolExecConfigs = configJson.getAsJsonObject("fhir")
                .getAsJsonObject("tools").getAsJsonObject(HealthCmdConstants.CMD_MODE_SERVICE_GEN);

        if (templateToolExecConfigs != null) {
            JsonObject templateToolExecConfig = templateToolExecConfigs.getAsJsonObject();
            CapabilityStatement capabilityStatement = processAndReturnCapabilityStatement();
            JsonObject smartConfiguration = processAndReturnSMARTConfig();

            try {
                ClassLoader classLoader = this.getClass().getClassLoader();
                String templateToolConfigClassName = "org.wso2.healthcare.fhir.codegen.ballerina.project.tool." +
                        "config.BallerinaProjectToolConfig";
                Class<?> templateToolConfigClazz = classLoader.loadClass(templateToolConfigClassName);
                String templateToolClassName = "org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectTool";
                Class<?> templateToolClazz = classLoader.loadClass(templateToolClassName);
                ToolConfig templateToolConfigInstance = (ToolConfig) templateToolConfigClazz.getConstructor().newInstance();
                templateToolConfigInstance.setTargetDir(targetOutputPath);
                templateToolConfigInstance.setToolName(HealthCmdConstants.CMD_MODE_TEMPLATE);

                templateToolConfigInstance.configure(new JsonConfigType(
                        templateToolExecConfig.getAsJsonObject().getAsJsonObject("config")));
                Tool templateTool = (Tool) templateToolClazz.getConstructor().newInstance();
                templateTool.initialize(templateToolConfigInstance);
                templateToolConfigInstance.overrideConfig("project.package.igConfig", populateIGConfig(
                                HealthCmdConstants.CMD_DEFAULT_IG_NAME,
                                orgName,
                                includedProfiles,
                                null
                        )
                );
                if (dependentPackage != null) {
                    JsonElement overrideConfig = new Gson().toJsonTree(dependentPackage);
                    JsonElement nameConfig =
                            new Gson().toJsonTree(dependentPackage.substring(dependentPackage.lastIndexOf('/') + 1));
                    templateToolConfigInstance.overrideConfig("project.package.dependentPackage", overrideConfig);
                    templateToolConfigInstance.overrideConfig("project.package.namePrefix", nameConfig);
                }
                String prebuiltServiceGenToolConfigClassName = "org.wso2.healthcare.fhir.ballerina.prebuiltservicegen.tool.ServiceGenToolConfig";
                Class<?> prebuiltServiceGenToolConfigClazz = classLoader.loadClass(prebuiltServiceGenToolConfigClassName);
                String prebuiltServiceGenToolClassName = "org.wso2.healthcare.fhir.ballerina.prebuiltservicegen.tool.ServiceGenTool";
                ToolConfig prebuiltServiceGenToolConfigInstance = (ToolConfig) prebuiltServiceGenToolConfigClazz.getConstructor().newInstance();
                prebuiltServiceGenToolConfigInstance.configure(new JsonConfigType(
                        serviceToolExecConfigs.getAsJsonObject().getAsJsonObject("config")));

                fhirToolLib.getToolImplementations().putIfAbsent(HealthCmdConstants.CMD_MODE_PACKAGE, templateTool);
                Properties toolProperties = new Properties();
                ((AbstractToolContext) fhirToolLib.getToolContext()).setCustomToolProperties(toolProperties);
                toolProperties.put("referenceServerCapabilities", capabilityStatement);
                toolProperties.put("referenceServerSmartConfiguration", smartConfiguration);
                toolProperties.put("packageInfoMap", serviceToolExecConfigs.getAsJsonObject().getAsJsonObject("config")
                        .getAsJsonObject("igPackageInfo"));

                TemplateGenerator mainTemplateGenerator = templateTool.execute(fhirToolLib.getToolContext());
                toolProperties.put("fhirServiceGenProperties", mainTemplateGenerator.getGeneratorProperties());
                prebuiltServiceGenToolConfigInstance.setTargetDir(targetOutputPath);
                prebuiltServiceGenToolConfigInstance.setToolName(HealthCmdConstants.CMD_MODE_SERVICE_GEN);
                JsonArray tokenEndpointAuthMethodsSupported = smartConfiguration.getAsJsonArray(
                        "token_endpoint_auth_methods_supported");
                List<String> authMethods = new ArrayList<>();
                tokenEndpointAuthMethodsSupported.forEach(authMethod -> {
                    authMethods.add(authMethod.getAsString());
                });
                if (authMethods.contains("client_secret_basic") && authMethods.contains("private_key_jwt")) {
                    Console console = System.console();
                    if (console != null) {
                        String input = console.readLine("The given SMART configuration supports " +
                                "both client_secret_basic and private_key_jwt authentication methods. " +
                                "Press 1 for client_secret_basic or Press 2 for private_key_jwt");
                        if ("1".equals(input)) {
                            authMethod = "client_secret_basic";
                        } else if ("2".equals(input)) {
                            authMethod = "private_key_jwt";
                        } else {
                            printStream.println("Invalid input. Exiting the process.");
                            HealthCmdUtils.exitError(true);
                        }
                    } else {
                        printStream.println("The given SMART configuration supports both client_secret_basic and " +
                                "private_key_jwt authentication methods. Choosing client_secret_basic as the default option.");
                        authMethod = "client_secret_basic";
                    }
                } else if (authMethods.contains("client_secret_basic")) {
                    authMethod = "client_secret_basic";
                } else if (authMethods.contains("private_key_jwt")) {
                    authMethod = "private_key_jwt";
                } else {
                    printStream.println("The given auth method is not supported by the SMART configuration.");
                    HealthCmdUtils.exitError(true);
                }
                prebuiltServiceGenToolConfigInstance.overrideConfig("servicegen.config.projectName", new Gson().toJsonTree(projectName));
                prebuiltServiceGenToolConfigInstance.overrideConfig("servicegen.config.serverName", new Gson().toJsonTree(fhirServerName));
                prebuiltServiceGenToolConfigInstance.overrideConfig("servicegen.config.authMethod", new Gson().toJsonTree(authMethod));

                Class<?> prebuiltServiceGenToolClazz = classLoader.loadClass(prebuiltServiceGenToolClassName);
                Tool serviceGenTool = (Tool) prebuiltServiceGenToolClazz.getConstructor().newInstance();
                serviceGenTool.initialize(prebuiltServiceGenToolConfigInstance);
                serviceGenTool.execute(fhirToolLib.getToolContext());
            } catch (CodeGenException | ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                printStream.println("Error occurred while generating prebuilt FHIR service.");
                HealthCmdUtils.exitError(true);
            }
        }
        return false;
    }

    private CapabilityStatement processAndReturnCapabilityStatement() {
        IBaseResource capabilityStatement = null;
        if (StringUtils.isNotEmpty(capabilityStatementPath)) {
            //check whether capability statement is a url or a file path
            if (capabilityStatementPath.startsWith("http")) {
                try {
                    String capabilityStatementContent = IOUtils.toString(
                            new java.net.URL(capabilityStatementPath), StandardCharsets.UTF_8);
                    capabilityStatement = FHIRSpecParser.parseDefinition(capabilityStatementContent);
                    if (capabilityStatement instanceof CapabilityStatement) {
                        ((CapabilityStatement) capabilityStatement).getRest().get(0).getResource().forEach(resource -> {
                            if (resource.getSupportedProfile().size() > 0) {
                                //merge the current includedProfiles array with the supported profiles
                                includedProfiles = Arrays.copyOf(includedProfiles, includedProfiles.length +
                                        resource.getSupportedProfile().size());
                            }
                        });
                    }
                } catch (Exception e) {
                    printStream.println("Error occurred while reading capability statement from the given url:" +
                            capabilityStatementPath);
                }
            } else {
                File capabilityStatementFile = new File(capabilityStatementPath);
                if (capabilityStatementFile.exists()) {
                    try {
                        capabilityStatement = FHIRSpecParser.parseDefinition(capabilityStatementFile);
                        if (capabilityStatement instanceof CapabilityStatement) {
                            ((CapabilityStatement) capabilityStatement).getRest().get(0).getResource().forEach(resource -> {
                                if (resource.getSupportedProfile().size() > 0) {
                                    if (includedProfiles == null) {
                                        includedProfiles = new String[0];
                                    }
                                    List<String> includedProfilesList = new ArrayList<>(Arrays.asList(includedProfiles));
                                    //merge the current includedProfiles array with the supported profiles
                                    for (CanonicalType profile : resource.getSupportedProfile()) {
                                        if (!includedProfilesList.contains(profile.getValue())) {
                                            includedProfilesList.add(profile.getValue());
                                        }
                                    }
                                    includedProfiles = includedProfilesList.toArray(new String[0]);
                                }

                            });
                            if (fhirServerName == null) {
                                String publisher = ((CapabilityStatement) capabilityStatement).getPublisher();
                                if (StringUtils.isNotEmpty(publisher)) {
                                    fhirServerName = publisher;
                                } else {
                                    printStream.println("Cannot extract FHIR server name from capability statement. " +
                                            "Manually set FHIR server name using --server-name option.");
                                    HealthCmdUtils.exitError(true);
                                }
                            }
                        }
                    } catch (Exception e) {
                        printStream.println("Error occurred while reading capability statement file.");
                    }
                } else {
                    printStream.println("Capability statement file does not exist in the given path.");
                }
            }
        }
        return (capabilityStatement instanceof CapabilityStatement) ? (CapabilityStatement) capabilityStatement : null;
    }

    private JsonObject processAndReturnSMARTConfig() {
        JsonObject smartConfig = null;
        if (StringUtils.isNotEmpty(smartConfigPath)) {
            //check whether smart config is a url or a file path
            if (smartConfigPath.startsWith("http")) {
                try {
                    String smartConfigContent = IOUtils.toString(
                            new java.net.URL(smartConfigPath), StandardCharsets.UTF_8);
                    smartConfig = new Gson().fromJson(smartConfigContent, JsonObject.class);
                } catch (Exception e) {
                    printStream.println("Error occurred while reading smart configuration from the given url:" +
                            smartConfigPath);
                }
            } else {
                File smartConfigFile = new File(smartConfigPath);
                if (smartConfigFile.exists()) {
                    try {
                        String smartConfigContent = IOUtils.toString(smartConfigFile.toURI(), StandardCharsets.UTF_8);
                        smartConfig = new Gson().fromJson(smartConfigContent, JsonObject.class);
                    } catch (Exception e) {
                        printStream.println("Error occurred while reading smart configuration file.");
                    }
                } else {
                    printStream.println("Smart configuration file does not exist in the given path.");
                }
            }
        }
        return smartConfig;
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
