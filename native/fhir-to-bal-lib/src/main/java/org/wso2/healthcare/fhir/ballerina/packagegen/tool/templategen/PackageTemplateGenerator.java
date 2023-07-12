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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.templategen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateContext;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.DependencyConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.PackageTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ResourceTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.VelocityUtil;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Generator for Package Template.
 */
public class PackageTemplateGenerator extends AbstractFHIRTemplateGenerator {

    private static final Log LOG = LogFactory.getLog(PackageTemplateGenerator.class);
    private final Map<String, Object> packageProperties = new HashMap<>();
    private VelocityUtil velocityUtil;
    private PackageTemplateContext packageTemplateContext;

    public PackageTemplateGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
        LOG.debug("Package Template Generator Initiated");
        ResourceTemplateGenerator resourceTemplateGenerator =
                new ResourceTemplateGenerator(targetDir);
        this.setChildTemplateGenerator(resourceTemplateGenerator);
        LOG.debug("Package Template Generator Ended");
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        LOG.debug("Started: Package Template Generation");
        this.packageTemplateContext = (PackageTemplateContext) generatorProperties.get("packageContext");
        BallerinaPackageGenToolConfig toolConfig = (BallerinaPackageGenToolConfig) generatorProperties.get(
                "toolConfig");
        this.velocityUtil = new VelocityUtil(toolConfig);

        String packagePath = this.getTargetDir() + File.separator + toolConfig.getPackageConfig().getName();
        String packageName = toolConfig.getPackageConfig().getName();
        this.packageProperties.put("packagePath", packagePath);
        this.packageProperties.put("packageName", packageName);
        this.packageProperties.put("packageIdentifier", packageName.substring(packageName.lastIndexOf(".") + 1));

        generateDefaultPackageStructure();

        if (Objects.equals(this.packageTemplateContext.getIgTemplateContext().getIgName(), ToolConstants.BASE_IG)) {
            this.packageProperties.put("isBasePackage", true);
            this.packageProperties.put("importIdentifier", "");
            bootstrapFromR4Base(toolConfig);
            generateModulesStructure();
            generatePackageModules();
        } else {
            this.packageProperties.put("isBasePackage", false);
            if (this.packageTemplateContext.getBasePackageName() != null) {
                String basePackage = this.packageTemplateContext.getBasePackageName();
                String basePackageIdentifier = basePackage.substring(basePackage.lastIndexOf(".") + 1);
                this.packageProperties.put("basePackage", basePackage);
                this.packageProperties.put("basePackageIdentifier", basePackageIdentifier);
                this.packageProperties.put("importIdentifier", basePackageIdentifier + ":");
            } else {
                this.packageProperties.put("isBasePackage", true);
                this.packageProperties.put("importIdentifier", "");
                bootstrapFromR4Base(toolConfig);
                generateModulesStructure();
                generatePackageModules();
            }
        }
        generatePackageEssentials(toolConfig);
        LOG.debug("Ended: Package Template Generation");
    }

    /**
     * Generates default package structure.
     *
     * @throws CodeGenException codegen exception
     */
    private void generateDefaultPackageStructure() throws CodeGenException {
        LOG.debug("Started: Default package Structure Generation");
        String packagePath = (String) this.packageProperties.get("packagePath");
        String packageResourcesPath = packagePath + File.separator + "resources";
        String packageTestsPath = packagePath + File.separator + "tests";

        CommonUtil.createNestedDirectory(packagePath);
        CommonUtil.createNestedDirectory(packageResourcesPath);
        CommonUtil.createNestedDirectory(packageTestsPath);

        this.packageProperties.put("packageResourcesPath", packageResourcesPath);
        this.packageProperties.put("packageTestsPath", packageTestsPath);
        LOG.debug("Ended: Default package Structure Generation");
    }

    /**
     * Generates modules structure.
     *
     * @throws CodeGenException codegen exception
     */
    private void generateModulesStructure() throws CodeGenException {
        LOG.debug("Started: Modules Generation");
        String packagePath = (String) this.packageProperties.get("packagePath");
        String packageModulesPath = packagePath + File.separator + "modules";
        String packageModulesParserPath = packagePath + File.separator + "modules" + File.separator + "parser";
        String packageModulesParserTestsPath = packageModulesParserPath + File.separator + "tests";
        String packageModulesTerminologyPath = packagePath + File.separator +
                "modules" + File.separator + "terminology";

        CommonUtil.createNestedDirectory(packageModulesPath);
        CommonUtil.createNestedDirectory(packageModulesParserPath);
        CommonUtil.createNestedDirectory(packageModulesParserTestsPath);
        CommonUtil.createNestedDirectory(packageModulesTerminologyPath);

        this.packageProperties.put("packageModulesPath", packageModulesPath);
        this.packageProperties.put("packageModulesParserPath", packageModulesParserPath);
        this.packageProperties.put("packageModulesParserTestsPath", packageModulesParserTestsPath);
        this.packageProperties.put("packageModulesTerminologyPath", packageModulesTerminologyPath);
        LOG.debug("Ended: Modules Generation");
    }

    /**
     * Generate package modules.
     *
     * @throws CodeGenException codegenException
     */
    private void generatePackageModules() throws CodeGenException {
        LOG.debug("Started: Package Modules Generation");
        TemplateContext moduleContext = this.getNewTemplateContext();
        moduleContext.setProperty("packageName", this.packageProperties.get("packageName"));
        moduleContext.setProperty("packageIdentifier", this.packageProperties.get("packageIdentifier"));

        String filePath = CommonUtil.generateFilePath((String) this.packageProperties.get(
                "packageModulesParserPath"), "gen_parser" + ToolConstants.BAL_EXTENSION, "");
        this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH + File.separator +
                        "module_parser.vm", moduleContext, "", filePath);

        filePath = CommonUtil.generateFilePath((String) this.packageProperties.get("packageModulesParserTestsPath"),
                "gen_tests_parser" + ToolConstants.BAL_EXTENSION, "");
        this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH + File.separator +
                        "module_parser_test_testsparser.vm", moduleContext, "", filePath);

        filePath = CommonUtil.generateFilePath((String) this.packageProperties.get("packageModulesParserTestsPath"),
                "gen_sample_resouce_models" + ToolConstants.BAL_EXTENSION, "");
        this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH + File.separator +
                        "module_parser_test_sampleresoucemodels.vm", moduleContext, "", filePath);

        filePath = CommonUtil.generateFilePath((String) this.packageProperties.get("packageModulesTerminologyPath"),
                "gen_terminology" + ToolConstants.BAL_EXTENSION, "");
        this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH + File.separator +
                        "module_terminology.vm", moduleContext, "", filePath);
        LOG.debug("Ended: Package Modules Generation");
    }

    /**
     * Generates package essential Ballerina files.
     *
     * @param toolConfig BallerinaPackageGenToolConfig
     * @throws CodeGenException codegenException
     */
    private void generatePackageEssentials(BallerinaPackageGenToolConfig toolConfig)
            throws CodeGenException {
        LOG.debug("Started: Package Essentials Generation");
        try {
            String packagePath = (String) this.packageProperties.get("packagePath");
            String filePath = CommonUtil.generateFilePath(packagePath, "Ballerina"
                    + ToolConstants.TOML_EXTENSION, "");
            this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH + File.separator + "ballerina_toml.vm",
                    this.createTemplateContextForBallerinaToml(toolConfig), "", filePath);

            filePath = CommonUtil.generateFilePath(packagePath, "Module"
                    + ToolConstants.MD_EXTENSION, "");
            this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH + File.separator + "module.vm",
                    this.createTemplateContextForModuleMD(), "", filePath);

            filePath = CommonUtil.generateFilePath(packagePath, "Package"
                    + ToolConstants.MD_EXTENSION, "");
            this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH + File.separator + "package.vm",
                    this.createTemplateContextForPackageMD(toolConfig), "", filePath);

            filePath = CommonUtil.generateFilePath(packagePath, "gen_initializer"
                    + ToolConstants.BAL_EXTENSION, "");
            this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH + File.separator + "initializer.vm",
                    this.createTemplateContextForInitializer(), "", filePath);

            filePath = CommonUtil.generateFilePath(packagePath, "gen_variables"
                    + ToolConstants.BAL_EXTENSION, "");
            this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH + File.separator + "variables.vm",
                    this.createTemplateContextForVariables(), "", filePath);
        } catch (CodeGenException e) {
            throw new CodeGenException("Error occurred while generating template artifacts for ballerina package ", e);
        }
        LOG.debug("Ended: Package Essentials Generation");
    }

    /**
     * Copies boostrap package contents from R4 base
     *
     * @param toolConfig BallerinaPackageGenToolConfig
     */
    private void bootstrapFromR4Base(BallerinaPackageGenToolConfig toolConfig) throws CodeGenException {
        LOG.debug("Started: R4 Base Package bootstrapping");
        String packagePath = (String) this.packageProperties.get("packagePath");
        try {
            // temporary fix to check if running from jar or IDE
            String className = this.getClass().getName().replace('.', '/');
            String classJar =
                    this.getClass().getResource("/" + className + ".class").toString();
            if (classJar.startsWith("jar:")) {
                // running from jar
                LOG.error("running from jar");
                String path = PackageTemplateGenerator.class.getProtectionDomain().getCodeSource().
                        getLocation().toURI().getPath();
                CommonUtil.copyContentFormJar(path, packagePath);
            } else {
                // running from IDE
                LOG.error("running from IDE");
                String r4BasePath = toolConfig.getResourceHomeDir() + File.separator + ToolConstants.R4_BASE_PATH;
                CommonUtil.copyContentsToDir(r4BasePath, packagePath);
            }

        } catch (URISyntaxException | IOException e) {
            throw new CodeGenException("Error occurred while copying R4 base package contents ", e);
        }
        LOG.debug("Ended: R4 Base Package bootstrapping");
    }

    /**
     * Creates velocity template contexts related to Ballerina toml.
     *
     * @param toolConfig BallerinaPackageGenToolConfig
     * @return velocity template context
     */
    private TemplateContext createTemplateContextForBallerinaToml(BallerinaPackageGenToolConfig toolConfig) {
        LOG.debug("Started: Ballerina.toml generation");
        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty("org", toolConfig.getPackageConfig().getOrg());
        templateContext.setProperty("packageName", this.packageProperties.get("packageName"));
        templateContext.setProperty("version", toolConfig.getPackageConfig().getVersion());
        templateContext.setProperty("distribution", toolConfig.getPackageConfig().getBallerinaDistribution());
        templateContext.setProperty("igName", this.packageTemplateContext.getIgTemplateContext().getIgName());

        List<DependencyConfig> dependencies = new ArrayList<>(toolConfig.getPackageConfig().getDependencyConfigList());
        templateContext.setProperty("dependencies", dependencies);
        LOG.debug("Ended: Ballerina.toml generation");
        return templateContext;
    }

    /**
     * Creates velocity template contexts related to module.md
     *
     * @return velocity template context
     */
    private TemplateContext createTemplateContextForModuleMD() {
        LOG.debug("Started: Module.md generation");
        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty("igName", this.packageTemplateContext.getIgTemplateContext().getIgName());
        LOG.debug("Ended: Module.md generation");
        return templateContext;
    }

    /**
     * Creates velocity template contexts related to package.md
     *
     * @return velocity template context
     */
    private TemplateContext createTemplateContextForPackageMD(BallerinaPackageGenToolConfig toolConfig) {
        LOG.debug("Started: Package.md generation");
        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty("newline", this.velocityUtil.getNewLine());
        templateContext.setProperty("packageName", this.packageProperties.get("packageName"));
        templateContext.setProperty("isBasePackage", this.packageProperties.get("isBasePackage"));
        templateContext.setProperty("org", toolConfig.getPackageConfig().getOrg());
        templateContext.setProperty("igName", this.packageTemplateContext.getIgTemplateContext().getIgName());
        templateContext.setProperty("packageVersion", toolConfig.getPackageConfig().getVersion());

        List<ResourceTemplateContext> resourceTemplateContexts = new ArrayList<>(this.packageTemplateContext.getResourceTemplateContextMap().values());
        templateContext.setProperty("profiles", resourceTemplateContexts);
        templateContext.setProperty("igUrl", resourceTemplateContexts.get(0).getProfile()
                .substring(0, resourceTemplateContexts.get(0).getProfile().indexOf("StructureDefinition")));
        LOG.debug("Ended: Package.md generation");
        return templateContext;
    }

    /**
     * Creates velocity template contexts related to package initializations
     *
     * @return velocity template context
     */
    private TemplateContext createTemplateContextForInitializer() {
        LOG.debug("Started: Initializer.bal generation");
        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty("util", this.velocityUtil);
        templateContext.setProperty("newline", this.velocityUtil.getNewLine());
        templateContext.setProperty("igTitle", this.packageTemplateContext.getIgTemplateContext().getTitle());
        templateContext.setProperty("igName", this.packageTemplateContext.getIgTemplateContext().getIgName());
        templateContext.setProperty("igCode", this.packageTemplateContext.getIgTemplateContext().getIgCode());
        templateContext.setProperty("profiles", this.packageTemplateContext.getResourceTemplateContextMap());

        double numberOfSearchParameterMaps = this.packageTemplateContext.getIgTemplateContext().getSearchParameters().size() / 100d;
        templateContext.setProperty("searchParameterMaps", Math.floor(numberOfSearchParameterMaps));
        templateContext.setProperty("basePackageIdentifier", this.packageProperties.get("basePackageIdentifier"));
        templateContext.setProperty("importIdentifier", this.packageProperties.get("importIdentifier"));

        Set<String> resourceDependencies = new HashSet<>();
        resourceDependencies.add((String) this.packageProperties.get("basePackage"));
        templateContext.setProperty("imports", resourceDependencies);
        LOG.debug("Ended: Initializer.bal generation");
        return templateContext;
    }

    /**
     * Creates velocity template contexts related to package variables
     *
     * @return velocity template context
     */
    private TemplateContext createTemplateContextForVariables() {
        LOG.debug("Started: Variables.bal generation");
        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty("newline", this.velocityUtil.getNewLine());
        templateContext.setProperty("igCode", this.packageTemplateContext.getIgTemplateContext().getIgCode().toUpperCase());
        templateContext.setProperty("isBasePackage", this.packageProperties.get("isBasePackage"));

        double numberOfSearchParameterMaps = this.packageTemplateContext.getIgTemplateContext().getSearchParameters().size() / 100d;
        templateContext.setProperty("searchParameterMaps", Math.floor(numberOfSearchParameterMaps));
        templateContext.setProperty("searchParams", new ArrayList<>(this.packageTemplateContext.getIgTemplateContext().getSearchParameters().values()));
        templateContext.setProperty("basePackageIdentifier", this.packageProperties.get("basePackageIdentifier"));
        templateContext.setProperty("importIdentifier", this.packageProperties.get("importIdentifier"));

        Set<String> resourceDependencies = new HashSet<>();
        resourceDependencies.add((String) this.packageProperties.get("basePackage"));
        templateContext.setProperty("imports", resourceDependencies);
        LOG.debug("Ended: Variables.bal generation");
        return templateContext;
    }
}
