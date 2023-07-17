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
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.PackageTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ResourceTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.VelocityUtil;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;

import java.io.File;
import java.util.*;

import static org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants.CONSTRAINTS_LIB_IMPORT;

/**
 * Generator class for resource related template context
 */
public class ResourceTemplateGenerator extends AbstractFHIRTemplateGenerator {

    private static final Log LOG = LogFactory.getLog(ResourceTemplateGenerator.class);
    private final Map<String, Object> resourceProperties = new HashMap<>();
    private VelocityUtil velocityUtil;
    private PackageTemplateContext packageTemplateContext;
    private List<ResourceTemplateContext> resourceTemplateContexts;

    public ResourceTemplateGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
        LOG.debug("Resource Template Generator Initiated");
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {
        LOG.debug("Started: Resource Templates Generation");
        this.packageTemplateContext = (PackageTemplateContext) generatorProperties.get("packageContext");
        BallerinaPackageGenToolConfig toolConfig = (BallerinaPackageGenToolConfig) generatorProperties.get("toolConfig");
        velocityUtil = new VelocityUtil(toolConfig);

        String packagePath = this.getTargetDir() + File.separator + toolConfig.getPackageConfig().getName();
        this.resourceProperties.put("packagePath", packagePath);

        if (Objects.equals(this.packageTemplateContext.getIgTemplateContext().getIgName(), ToolConstants.BASE_IG)) {
            this.resourceProperties.put("isBasePackage", true);
            this.resourceProperties.put("importIdentifier", "");
        } else {
            this.resourceProperties.put("isBasePackage", false);
            if (this.packageTemplateContext.getBasePackageName() != null) {
                String basePackage = this.packageTemplateContext.getBasePackageName();
                String basePackageIdentifier = basePackage.substring(basePackage.lastIndexOf(".") + 1);
                this.resourceProperties.put("basePackage", basePackage);
                this.resourceProperties.put("basePackageIdentifier", basePackageIdentifier);
                this.resourceProperties.put("importIdentifier", basePackageIdentifier + ":");
            }
        }

        this.resourceTemplateContexts = new ArrayList<>(this.packageTemplateContext.getResourceTemplateContextMap().values());
        generateFHIRResources();
        LOG.debug("Ended: Resource Templates Generation");
    }

    /**
     * Generate FHIR resources
     *
     * @throws CodeGenException codeGenException
     */
    private void generateFHIRResources()
            throws CodeGenException {
        LOG.debug("Started: FHIR Resources Generation");
        try {
            String packagePath = (String) this.resourceProperties.get("packagePath");
            for (ResourceTemplateContext resourceTemplateContext : this.resourceTemplateContexts) {
                if (resourceTemplateContext.getResourceType().equals("Bundle"))
                    continue;

                String filePath = CommonUtil.generateFilePath(packagePath, "gen_resource_"
                        + CommonUtil.camelToSnake(resourceTemplateContext.getResourceDefinitionAnnotation().getName())
                        + ToolConstants.BAL_EXTENSION, "");

                this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH +
                                File.separator + "fhir_resource.vm", this.createTemplateContextForResourceSkeletons(
                                        resourceTemplateContext, this.packageTemplateContext), "", filePath);
            }
        } catch (CodeGenException e) {
            throw new CodeGenException("Error occurred while generating template artifacts for fhir resource", e);
        }
        LOG.debug("Ended: FHIR Resources Generation");
    }

    /**
     * Creates velocity template contexts for FHIR resources
     *
     * @param resourceTemplateContext resource template context
     * @return velocity template context
     */
    private TemplateContext createTemplateContextForResourceSkeletons(ResourceTemplateContext resourceTemplateContext,
                                                                      PackageTemplateContext packageContext) {
        TemplateContext templateContext = this.getNewTemplateContext();
        templateContext.setProperty("util", this.velocityUtil);
        templateContext.setProperty("newline", this.velocityUtil.getNewLine());
        templateContext.setProperty("licenseYear", ToolConstants.LICENSE_YEAR);
        templateContext.setProperty("resourceType", resourceTemplateContext.getResourceType());
        templateContext.setProperty("resourceName", resourceTemplateContext.getResourceName());
        templateContext.setProperty("profile", resourceTemplateContext.getProfile());
        templateContext.setProperty("baseType", resourceTemplateContext.getResourceDefinitionAnnotation().getBaseType());
        templateContext.setProperty("annotationElements", resourceTemplateContext.getResourceDefinitionAnnotation().getElements());
        templateContext.setProperty("resourceElements", resourceTemplateContext.getElements());
        templateContext.setProperty("extendedElements", resourceTemplateContext.getExtendedElements());

        packageContext.getDataTypesRegistry().add("boolean");
        packageContext.getDataTypesRegistry().add("string");
        packageContext.getDataTypesRegistry().add("decimal");
        templateContext.setProperty("dataTypes", packageContext.getDataTypesRegistry());

        templateContext.setProperty("isBasePackage", this.resourceProperties.get("isBasePackage"));
        templateContext.setProperty("basePackageIdentifier", this.resourceProperties.get("basePackageIdentifier"));
        templateContext.setProperty("importIdentifier", this.resourceProperties.get("importIdentifier"));

        Set<String> resourceDependencies = new TreeSet<>();
        if (!(boolean)this.resourceProperties.get("isBasePackage"))
            resourceDependencies.add((String) this.resourceProperties.get("basePackage"));

        Optional<String> dependency = resourceTemplateContext.getResourceDependencies()
                .stream()
                .filter(d -> d.equals(CONSTRAINTS_LIB_IMPORT))
                .findFirst();

        if (dependency.isPresent()) {
            resourceDependencies.add(dependency.get());
        }

        templateContext.setProperty("imports", resourceDependencies);

        return templateContext;
    }
}
