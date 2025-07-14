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

import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.BallerinaProjectConstants;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.AggregatedService;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model.BallerinaService;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Generator class to wrap all the generator classes in Ballerina project generator.
 */
public class BallerinaProjectGenerator extends AbstractFHIRTemplateGenerator {

    public BallerinaProjectGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {

        BallerinaProjectToolConfig ballerinaProjectToolConfig = (BallerinaProjectToolConfig) generatorProperties.get("config");
        Map<String, BallerinaService> serviceMap = (Map<String, BallerinaService>) generatorProperties.get("serviceMap");
        Map<String, AggregatedService> aggregatedServiceMap = (Map<String, AggregatedService>) generatorProperties.get("aggregatedServiceMap");
        Map<String, String> dependenciesMap = (Map<String, String>) generatorProperties.get("dependenciesMap");
        //evaluate usage of ? typed map as generator properties.

        String packagePath = this.getTargetDir();
        // Provide option to check and overwrite the existing package
        Console console = System.console();
        if (console != null && Files.exists(Paths.get(packagePath))) {
            String input = console.readLine("Generated templates already exists. Do you want to overwrite? (y/n): ");
            if ("n".equalsIgnoreCase(input)) {
                System.exit(0);
            } else if ("y".equalsIgnoreCase(input)) {
                System.out.println(BallerinaProjectConstants.PrintStrings.OVERWRITING_EXISTING_TEMPLATES);
            } else {
                System.out.println(BallerinaProjectConstants.PrintStrings.INVALID_INPUT);
                System.exit(0);
            }
        }

        // Generate individual services if aggregated API is not enabled
        if (!ballerinaProjectToolConfig.isEnableAggregatedApi()) {
            for (Map.Entry<String, BallerinaService> entry : serviceMap.entrySet()) {
                Map<String, Object> projectProperties = new HashMap<>();
                projectProperties.put("service", entry.getValue());
                projectProperties.put("resourceType", entry.getKey());
                projectProperties.put("config", ballerinaProjectToolConfig);
                projectProperties.put("dependencies", dependenciesMap);

                String basePackage = dependenciesMap.get("basePackage");
                String servicePackage = dependenciesMap.get("servicePackage");
                String igPackage = dependenciesMap.get("igPackage");
                String dependentPackage = dependenciesMap.get("dependentPackage");
                projectProperties.put("basePackageImportIdentifier", basePackage.substring(basePackage.lastIndexOf(".") + 1));
                projectProperties.put("servicePackageImportIdentifier", servicePackage.substring(servicePackage.lastIndexOf(".") + 1));
                projectProperties.put("igPackageImportIdentifier", igPackage.substring(igPackage.lastIndexOf(".") + 1));
                projectProperties.put("dependentPackageImportIdentifier", dependentPackage.substring(dependentPackage.lastIndexOf(".") + 1));
                projectProperties.put("projectAPIPath", this.getTargetDir() + entry.getKey().toLowerCase());

                ServiceGenerator balServiceGenerator = new ServiceGenerator(this.getTargetDir());
                balServiceGenerator.generate(toolContext, projectProperties);

                TomlGenerator tomlGenerator = new TomlGenerator(this.getTargetDir());
                tomlGenerator.generate(toolContext, projectProperties);

                MetaGenerator metaGenerator = new MetaGenerator(this.getTargetDir());
                metaGenerator.generate(toolContext, projectProperties);

                OasGenerator oasGenerator = new OasGenerator(this.getTargetDir());
                oasGenerator.generate(toolContext, projectProperties);

                ComponentYamlGenerator componentYamlGenerator = new ComponentYamlGenerator(this.getTargetDir());
                componentYamlGenerator.generate(toolContext, projectProperties);
            }
        } else {
            // Generate aggregated services
            for (Map.Entry<String, AggregatedService> entry : aggregatedServiceMap.entrySet()) {
                Map<String, Object> projectProperties = new HashMap<>();
                projectProperties.put("aggregatedService", entry.getValue());
                projectProperties.put("config", ballerinaProjectToolConfig);
                projectProperties.put("dependencies", dependenciesMap);

                String basePackage = dependenciesMap.get("basePackage");
                String servicePackage = dependenciesMap.get("servicePackage");
                String igPackage = dependenciesMap.get("igPackage");
                String dependentPackage = dependenciesMap.get("dependentPackage");
                projectProperties.put("basePackageImportIdentifier", basePackage.substring(basePackage.lastIndexOf(".") + 1));
                projectProperties.put("servicePackageImportIdentifier", servicePackage.substring(servicePackage.lastIndexOf(".") + 1));
                projectProperties.put("igPackageImportIdentifier", igPackage.substring(igPackage.lastIndexOf(".") + 1));
                projectProperties.put("dependentPackageImportIdentifier", dependentPackage.substring(dependentPackage.lastIndexOf(".") + 1));
                projectProperties.put("projectAPIPath", this.getTargetDir() + "fhir-service");

                AggregatedServiceGenerator aggregatedServiceGenerator = new AggregatedServiceGenerator(this.getTargetDir());
                aggregatedServiceGenerator.generate(toolContext, projectProperties);

                // Generate other files for the aggregated service
                TomlGenerator tomlGenerator = new TomlGenerator(this.getTargetDir());
                tomlGenerator.generate(toolContext, projectProperties);

                MetaGenerator metaGenerator = new MetaGenerator(this.getTargetDir());
                metaGenerator.generate(toolContext, projectProperties);

                // Generate OAS files for each service
                for (BallerinaService service : entry.getValue().getServices().values()) {
                    projectProperties.put("service", service);
                    projectProperties.put("resourceType", service.getName());
                    OasGenerator oasGenerator = new OasGenerator(this.getTargetDir());
                    oasGenerator.generate(toolContext, projectProperties);
                }
                
                // Generate component.yaml with all endpoints (called only once)
                ComponentYamlGenerator componentYamlGenerator = new ComponentYamlGenerator(this.getTargetDir());
                componentYamlGenerator.generate(toolContext, projectProperties);
            }
        }
    }
}
