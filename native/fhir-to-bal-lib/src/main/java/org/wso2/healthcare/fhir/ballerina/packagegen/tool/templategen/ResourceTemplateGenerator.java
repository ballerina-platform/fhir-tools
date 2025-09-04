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
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DataTypeProfile;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.PackageTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ResourceTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;

import java.io.File;
import java.util.*;

/**
 * Generator class for resource related template context
 */
public class ResourceTemplateGenerator extends AbstractFHIRTemplateGenerator {

    private static final Log LOG = LogFactory.getLog(ResourceTemplateGenerator.class);
    private final Map<String, Object> resourceProperties = new HashMap<>();
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

        String packagePath = this.getTargetDir() + File.separator + toolConfig.getPackageConfig().getName();
        this.resourceProperties.put("packagePath", packagePath);

        if (this.packageTemplateContext.getBasePackageName() != null) {
            this.resourceProperties.put("isBasePackage", false);
            String basePackage = this.packageTemplateContext.getBasePackageName();
            String basePackageIdentifier = basePackage.substring(basePackage.lastIndexOf(".") + 1);
            this.resourceProperties.put("basePackage", basePackage);
            this.resourceProperties.put("basePackageIdentifier", basePackageIdentifier);
            this.resourceProperties.put("importIdentifier", basePackageIdentifier + ":");
        }

        if (this.packageTemplateContext.getInternationalPackageName() != null) {
            this.resourceProperties.put("isInternationalPackage", false);
            String internationalPackage = this.packageTemplateContext.getInternationalPackageName();
            String internationalPackageIdentifier = internationalPackage.substring(internationalPackage.lastIndexOf(".") + 1);
            this.resourceProperties.put("internationalPackage", internationalPackage);
            this.resourceProperties.put("internationalPackageIdentifier", internationalPackageIdentifier);
            this.resourceProperties.put("internationalImportIdentifier", internationalPackageIdentifier + ":");
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

                String filePath = CommonUtil.generateFilePath(packagePath, "resource_"
                        + CommonUtil.camelToSnake(resourceTemplateContext.getResourceDefinitionAnnotation().getName())
                        + ToolConstants.BAL_EXTENSION, "");

                this.getTemplateEngine().generateOutputAsFile(ToolConstants.TEMPLATE_PATH +
                        ToolConstants.RESOURCE_PATH_SEPERATOR + "fhir_resource.vm", this.createTemplateContextForResourceSkeletons(
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
        templateContext.setProperty("util", GeneratorUtils.getInstance());
        templateContext.setProperty("newline", GeneratorUtils.getInstance().getNewLine());
        templateContext.setProperty("licenseYear", ToolConstants.LICENSE_YEAR);
        templateContext.setProperty("resourceType", resourceTemplateContext.getResourceType());
        templateContext.setProperty("resourceName", resourceTemplateContext.getResourceName());
        templateContext.setProperty("profile", resourceTemplateContext.getProfile());
        templateContext.setProperty("baseType", resourceTemplateContext.getResourceDefinitionAnnotation().getBaseType());
        templateContext.setProperty("annotationElements", resourceTemplateContext.getResourceDefinitionAnnotation().getElements());
        templateContext.setProperty("resourceElements", resourceTemplateContext.getResourceElements());
        templateContext.setProperty("sliceElements", resourceTemplateContext.getSliceElements());
        templateContext.setProperty("extendedElements", resourceTemplateContext.getExtendedElements());
        templateContext.setProperty("INT_MAX", Integer.MAX_VALUE);
        templateContext.setProperty("dataTypes", packageContext.getDataTypesRegistry());

        templateContext.setProperty("isBasePackage", this.resourceProperties.get("isBasePackage"));
        templateContext.setProperty("basePackageIdentifier", this.resourceProperties.get("basePackageIdentifier"));
        templateContext.setProperty("importIdentifier", this.resourceProperties.get("importIdentifier"));

        templateContext.setProperty("isInternationalPackage", this.resourceProperties.get("isInternationalPackage"));
        templateContext.setProperty("internationalPackageIdentifier", this.resourceProperties.get("internationalPackageIdentifier"));
        templateContext.setProperty("internationalImportIdentifier", this.resourceProperties.get("internationalImportIdentifier"));

        Set<String> resourceDependencies = new TreeSet<>();
        if (!(boolean) this.resourceProperties.get("isBasePackage")) {
            resourceDependencies.add((String) this.resourceProperties.get("basePackage"));
        }

        resourceDependencies.addAll(resourceTemplateContext.getResourceDependencies());

        // Store element datatype prefixes other than baseImport and internationalImport
        Set<String> prefixes = new HashSet<>();
        for (Element element : resourceTemplateContext.getSnapshotElements().values()) {
            Map<String, DataTypeProfile> profiles = element.getProfiles();
            for (Map.Entry<String, DataTypeProfile> entry : profiles.entrySet()) {
                if (entry.getValue().getPrefix() != null && !element.getPath().contains("extension")) {
                    prefixes.add(entry.getValue().getPrefix());
                }
            }
        }

        // Remove unused packages from dependent-ig mode
        if (prefixes.isEmpty()) {
            Iterator<String> iterator = resourceDependencies.iterator();
            while (iterator.hasNext()) {
                String dependency = iterator.next();
                if (dependency.endsWith(this.resourceProperties.get("basePackageIdentifier").toString())
                        || dependency.endsWith(this.resourceProperties.get("internationalPackageIdentifier").toString())
                        || dependency.contains("constraint")) {
                } else {
                    iterator.remove();
                }
            }
        }

        // remove international package if the current IG is the international package
        if (packageContext.getIgTemplateContext().getIgName().contains(this.resourceProperties.get("internationalPackageIdentifier").toString())) {
            resourceDependencies.remove(this.packageTemplateContext.getInternationalPackageName());
        }

        templateContext.setProperty("imports", resourceDependencies);
        return templateContext;
    }
}
