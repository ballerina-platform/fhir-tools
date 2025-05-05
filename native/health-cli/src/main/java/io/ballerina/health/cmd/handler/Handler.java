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

import com.google.gson.JsonObject;
import io.ballerina.health.cmd.core.utils.ErrorMessages;
import io.ballerina.health.cmd.core.utils.HealthCmdConstants;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import org.wso2.healthcare.codegen.tool.framework.commons.core.AbstractTool;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRSpecParser;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.FHIRSpecParserFactory;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.FHIRTool;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.config.FHIRToolConfig;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;

/**
 * Common interface for tool handlers.
 */
public interface Handler {

    /**
     * Initialize the protocol library.
     *
     * @param printStream       PrintStream to print the output
     * @param specificationPath Path to the specification
     */
    default AbstractTool initializeLib(String libName, PrintStream printStream, JsonObject configJson, String specificationPath) {

        if (HealthCmdConstants.CMD_SUB_FHIR.equals(libName)) {
            JsonConfigType toolConfig;
            FHIRTool fhirToolLib;
            FHIRToolConfig fhirToolConfig = new FHIRToolConfig();
            String fhirVersion;

            try {
                fhirVersion = configJson.getAsJsonObject("fhir").getAsJsonObject("tools")
                        .getAsJsonObject("template").getAsJsonObject("config")
                        .getAsJsonObject("fhir").get("version").getAsString();

                toolConfig = new JsonConfigType(configJson);
                fhirToolLib = new FHIRTool(fhirVersion);
                fhirToolConfig.configure(toolConfig);

                fhirToolConfig.setSpecBasePath(specificationPath);
                fhirToolLib.initialize(fhirToolConfig);

                AbstractFHIRSpecParser specParser = FHIRSpecParserFactory.getParser(fhirVersion);
                specParser.parseIG(fhirToolConfig, HealthCmdConstants.CMD_DEFAULT_IG_NAME, specificationPath);
                return fhirToolLib;

            } catch (CodeGenException e) {
                printStream.println(ErrorMessages.LIB_INITIALIZING_FAILED + Arrays.toString(e.getStackTrace())
                        + e.getMessage());
                HealthCmdUtils.throwLauncherException(e);
            }
        } else if (HealthCmdConstants.CMD_SUB_HL7.equals(libName)) {
            printStream.println(ErrorMessages.LIB_INITIALIZING_FAILED + "HL7 library is not supported yet");
        } else if (HealthCmdConstants.CMD_SUB_CDS.equals(libName)) {
            printStream.println("CDS library has been initialised");
        } else {
            printStream.println(ErrorMessages.LIB_INITIALIZING_FAILED + "Unknown library name");
        }
        return null;
    }

    void init(PrintStream printStream, String specificationPath);

    void setArgs(Map<String,Object> argsMap);

    boolean execute(String specificationPath, String targetOutputPath);
}
