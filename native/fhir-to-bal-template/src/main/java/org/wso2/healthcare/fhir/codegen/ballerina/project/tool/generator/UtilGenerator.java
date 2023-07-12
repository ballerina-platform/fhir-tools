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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.generator;

import org.wso2.healthcare.codegen.tooling.common.core.TemplateContext;
import org.wso2.healthcare.codegen.tooling.common.core.ToolContext;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.GeneratedUtil;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.ImplFunction;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.Parameter;
import org.wso2.healthcare.fhir.codegen.tool.lib.AbstractFHIRTemplateGenerator;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

/**
 * Generator for Utility functions file.
 */
public class UtilGenerator extends AbstractFHIRTemplateGenerator {

    public UtilGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        String directoryPath = this.getTargetDir() + generatorProperties.get("resourceType") + "API" + File.separator;
        this.getTemplateEngine().generateOutputAsFile(BallerinaProjectConstants.RESOURCE_PATH_TEMPLATES +
                File.separator + "generatedUtil.vm", createTemplateContextForBalUtil(
                generatorProperties), directoryPath, "generated.bal");
    }

    private TemplateContext createTemplateContextForBalUtil(Map<String, Object> generatorProperties) {

        org.wso2.healthcare.codegen.tooling.common.core.TemplateContext templateContext = this.getNewTemplateContext();
        GeneratedUtil generatedUtil = initializeUtilWithDefaults(generatorProperties);
        templateContext.setProperty("utilImpl", generatedUtil);
        return templateContext;
    }

    private GeneratedUtil initializeUtilWithDefaults(Map<String, Object> generatorProperties) {

        BallerinaProjectToolConfig ballerinaProjectToolConfig = (BallerinaProjectToolConfig) generatorProperties.get(
                "config");
        String resourceType = ((String) generatorProperties.get("resourceType"));
        BallerinaService service = (BallerinaService) generatorProperties.get("service");
        String fhirVersion = ballerinaProjectToolConfig.getFhirVersion();
        GeneratedUtil generatedUtil = new GeneratedUtil(resourceType, fhirVersion);
        generatedUtil.setInteractionImpl(ballerinaProjectToolConfig);
        for (Map.Entry entry : generatedUtil.getInteractionImpl().entrySet()) {
            ImplFunction implFunction = new ImplFunction((String) entry.getKey(),
                    (String) entry.getValue(), fhirVersion);
            switch (entry.getKey().toString()) {
                case "create":
                    implFunction.setInputParams(Arrays.asList(
                            new Parameter("resourceEntity", fhirVersion, "FHIRResourceEntity"),
                            new Parameter("ctx", "http", "RequestContext")));
                    implFunction.setOutputParams(Arrays.asList(
                            new Parameter("", "", "string"),
                            new Parameter("", fhirVersion, "FHIRError")));
                    implFunction.getSourceConnectFunction().setInputParams(Arrays.asList(
                            new Parameter("resourceEntity", fhirVersion, "FHIRResourceEntity"),
                            new Parameter("fhirContext", fhirVersion, "FHIRContext")));
                    implFunction.getSourceConnectFunction().setOutputParams(Arrays.asList(
                            new Parameter("", "", "string"),
                            new Parameter("", fhirVersion, "FHIRError")));
                    break;
                case "read":
                    implFunction.setInputParams(Arrays.asList(
                            new Parameter("id", "", "string"),
                            new Parameter("ctx", "http", "RequestContext")));
                    implFunction.setOutputParams(Arrays.asList(
                            new Parameter("", fhirVersion, "FHIRResourceEntity"),
                            new Parameter("", fhirVersion, "FHIRError")));
                    implFunction.getSourceConnectFunction().setInputParams(Arrays.asList(
                            new Parameter("id", "", "string"),
                            new Parameter("fhirContext", "r4", "FHIRContext")));
                    implFunction.getSourceConnectFunction().setOutputParams(Arrays.asList(
                            new Parameter("", "", service.getName()),
                            new Parameter("", fhirVersion, "FHIRError")));
                    break;
                case "update":
                    implFunction.setInputParams(Arrays.asList(
                            new Parameter("resourceEntity", fhirVersion, "FHIRResourceEntity"),
                            new Parameter("id", "", "string"),
                            new Parameter("ctx", "http", "RequestContext")));
                    implFunction.getSourceConnectFunction().setInputParams(Arrays.asList(
                            new Parameter("id", "", "string"),
                            new Parameter("fhirContext", fhirVersion, "FHIRContext")));
                    implFunction.getSourceConnectFunction().setOutputParams(Arrays.asList(
                            new Parameter("", "", service.getName()),
                            new Parameter("", fhirVersion, "FHIRError")));
                    break;
                case "search":
                    implFunction.setInputParams(Arrays.asList(
                            new Parameter("params", "map<r4", "RequestSearchParameter[]>"),
                            new Parameter("ctx", "http", "RequestContext")));
                    implFunction.setOutputParams(Arrays.asList(
                            new Parameter("", fhirVersion, "BundleEntry[]"),
                            new Parameter("", fhirVersion, "FHIRError")));
                    implFunction.getSourceConnectFunction().setInputParams(Arrays.asList(
                            new Parameter("params", "map<r4", "RequestSearchParameter[]>"),
                            new Parameter("fhirContext", fhirVersion, "FHIRContext")));
                    implFunction.getSourceConnectFunction().setOutputParams(Arrays.asList(
                            new Parameter("", fhirVersion, "Bundle"),
                            new Parameter("", "", service.getName() + "[]"),
                            new Parameter("", fhirVersion, "FHIRError")));
                    break;

            }
            generatedUtil.addImplFunction(implFunction);
        }
        generatedUtil.setDefaultProfile(service.getProfileList().get(0).getUrl());
        generatedUtil.addImport("wso2healthcare/healthcare.fhir.r4");
        generatedUtil.addImport("ballerina/lang.value");
        generatedUtil.addImport("ballerina/log");
        generatedUtil.addImport("ballerina/http");

        return generatedUtil;
    }
}
