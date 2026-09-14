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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

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
        // Provide option to check and overwrite existing template output (not staging dirs alone)
        Console console = System.console();
        if (console != null && hasExistingGeneratedTemplates(packagePath, ballerinaProjectToolConfig, serviceMap)) {
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

                String projectAPIPath = this.getTargetDir() + entry.getKey().toLowerCase();
                String templateName = ballerinaProjectToolConfig.getVersionConfig().getNamePrefix() + "." +
                        entry.getKey().toLowerCase();
                String basePackage = dependenciesMap.get("basePackage");
                String servicePackage = dependenciesMap.get("servicePackage");
                String igPackage = dependenciesMap.get("igPackage");
                String dependentPackage = resolveDependentPackageImport(
                        ballerinaProjectToolConfig, dependenciesMap, templateName);
                dependenciesMap.put("dependentPackage", dependentPackage);
                projectProperties.put("basePackageImportIdentifier", getImportIdentifier(basePackage));
                projectProperties.put("servicePackageImportIdentifier", getImportIdentifier(servicePackage));
                projectProperties.put("igPackageImportIdentifier", getImportIdentifier(igPackage));
                projectProperties.put("dependentPackageImportIdentifier", getImportIdentifier(dependentPackage));
                projectProperties.put("projectAPIPath", projectAPIPath);

                if (ballerinaProjectToolConfig.isGenerateIgModuleEnabled()) {
                    generateIgModule(projectAPIPath, ballerinaProjectToolConfig, serviceMap, null);
                }

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
                String templateName = ballerinaProjectToolConfig.getTemplatePackageName();
                String dependentPackage = resolveDependentPackageImport(
                        ballerinaProjectToolConfig, dependenciesMap, templateName);
                dependenciesMap.put("dependentPackage", dependentPackage);
                projectProperties.put("basePackageImportIdentifier", getImportIdentifier(basePackage));
                projectProperties.put("servicePackageImportIdentifier", getImportIdentifier(servicePackage));
                projectProperties.put("igPackageImportIdentifier", getImportIdentifier(igPackage));
                projectProperties.put("dependentPackageImportIdentifier", getImportIdentifier(dependentPackage));

                // Minimal and --flat both skip the fhir-service/ nesting; minimal additionally skips
                // Ballerina.toml, OAS, .choreo, and .gitignore generation below.
                if (ballerinaProjectToolConfig.isMinimalGeneration() || ballerinaProjectToolConfig.isFlatOutput()) {
                    projectProperties.put("projectAPIPath", this.getTargetDir());
                } else {
                    projectProperties.put("projectAPIPath", this.getTargetDir() + "fhir-service");
                }
                if (ballerinaProjectToolConfig.isGenerateIgModuleEnabled()) {
                    generateIgModule(projectProperties.get("projectAPIPath").toString(),
                            ballerinaProjectToolConfig, serviceMap, entry.getValue());
                }

                AggregatedServiceGenerator aggregatedServiceGenerator = new AggregatedServiceGenerator(this.getTargetDir());
                aggregatedServiceGenerator.generate(toolContext, projectProperties);

                // Generate other files for the aggregated service
                // Skip Toml generation in minimal mode
                if (!ballerinaProjectToolConfig.isMinimalGeneration()) {
                    TomlGenerator tomlGenerator = new TomlGenerator(this.getTargetDir());
                    tomlGenerator.generate(toolContext, projectProperties);
                }

                MetaGenerator metaGenerator = new MetaGenerator(this.getTargetDir());
                metaGenerator.generate(toolContext, projectProperties);

                // Skip OAS and Component YAML generation in minimal mode
                if (!ballerinaProjectToolConfig.isMinimalGeneration()) {
                    // Generate OAS files for each service
                    Set<String> resourceTypes = new HashSet<>();
                    Set<BallerinaService> services = new HashSet<>();
                    for (BallerinaService service : entry.getValue().getServices().values()) {
                        services.add(service);
                        resourceTypes.add(service.getName());
                    }
                    projectProperties.put("resourceTypes", resourceTypes);
                    projectProperties.put("services", services);
                    OasGenerator oasGenerator = new OasGenerator(this.getTargetDir());
                    oasGenerator.generate(toolContext, projectProperties);

                    // Generate component.yaml with all endpoints (called only once)
                    ComponentYamlGenerator componentYamlGenerator = new ComponentYamlGenerator(this.getTargetDir());
                    componentYamlGenerator.generate(toolContext, projectProperties);
                }
            }
        }
    }

    private boolean hasExistingGeneratedTemplates(String packagePath, BallerinaProjectToolConfig config,
                                                Map<String, BallerinaService> serviceMap) {
        Path basePath = Paths.get(packagePath);
        if (!Files.isDirectory(basePath)) {
            return false;
        }
        if (config.isEnableAggregatedApi()) {
            Path projectPath = (config.isMinimalGeneration() || config.isFlatOutput())
                    ? basePath : basePath.resolve("fhir-service");
            return Files.exists(projectPath.resolve("service.bal"));
        }
        if (serviceMap == null || serviceMap.isEmpty()) {
            return false;
        }
        for (String resourceType : serviceMap.keySet()) {
            if (Files.exists(basePath.resolve(resourceType.toLowerCase()).resolve("service.bal"))) {
                return true;
            }
        }
        return false;
    }

    private String resolveDependentPackageImport(BallerinaProjectToolConfig config, Map<String, String> dependenciesMap,
                                                 String templateName) {
        if (!config.isGenerateIgModuleEnabled()) {
            return dependenciesMap.get("dependentPackage");
        }
        String igModuleName = config.getGenerateIgModuleName();
        return templateName + "." + igModuleName;
    }

    private String getImportIdentifier(String importStatement) {
        if (importStatement == null || importStatement.isEmpty()) {
            return "";
        }
        int slashIndex = importStatement.lastIndexOf('/');
        String modulePath = slashIndex >= 0 ? importStatement.substring(slashIndex + 1) : importStatement;
        int dotIndex = modulePath.lastIndexOf('.');
        return dotIndex >= 0 ? modulePath.substring(dotIndex + 1) : modulePath;
    }

    private void generateIgModule(String projectAPIPath, BallerinaProjectToolConfig config,
                                  Map<String, BallerinaService> serviceMap,
                                  AggregatedService aggregatedService) throws CodeGenException {
        String igModuleName = config.getGenerateIgModuleName();
        if (igModuleName == null || igModuleName.isEmpty()) {
            return;
        }
        Path modulePath = Paths.get(projectAPIPath, "modules", igModuleName);
        try {
            Files.createDirectories(modulePath);
        } catch (IOException e) {
            throw new CodeGenException("Error creating IG module directory: " + e.getMessage(), e);
        }
        String generateIgModuleSourceDir = config.getGenerateIgModuleSourceDir();
        if (generateIgModuleSourceDir == null || generateIgModuleSourceDir.isEmpty()) {
            throw new CodeGenException("IG module source directory is not set.");
        }
        Path sourceDirectory = Paths.get(generateIgModuleSourceDir);
        try {
            try (Stream<Path> sourceFiles = Files.walk(sourceDirectory)) {
                sourceFiles
                        .filter(path -> !Files.isDirectory(path))
                        .filter(path -> path.toString().endsWith(".bal")
                                || path.toString().endsWith("Package.md")
                                || path.toString().contains("resources" + java.io.File.separator))
                        .forEach(path -> {
                            Path relativePath = sourceDirectory.relativize(path);
                            Path destination = modulePath.resolve(relativePath);
                            try {
                                Files.createDirectories(destination.getParent());
                                Files.copy(path, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw new CodeGenException("Error generating IG module: " + e.getCause().getMessage(),
                        e.getCause());
            }
            throw e;
        } catch (IOException e) {
            throw new CodeGenException("Error generating IG module: " + e.getMessage(), e);
        }
    }
}