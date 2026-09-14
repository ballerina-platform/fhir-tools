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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model;

import com.google.gson.JsonObject;
import org.apache.commons.text.CaseUtils;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.IncludedIGConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FHIRProfile<StructureDefinition> {
    // Mirrors io.ballerina.health.cmd.core.utils.IgModuleNameUtils#isValidBallerinaModuleName -- fhir-to-bal-template
    // can't depend on health-cli (build order is the reverse), so a configured module name is re-validated here
    // rather than trusted, since it can also arrive via raw tool-config JSON, bypassing the CLI's --ig-module-name
    // validation.
    private static final String MODULE_NAME_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]*$";

    private String parentRef;
    private boolean isAbstract;
    private String name;
    private String fhirVersion;
    private String url;
    private Set<String> importsList;
    private Map<String, JsonObject> examples = new HashMap<>();
    private String igName;
    private String resourceType;
    private String packagePrefix;

    public FHIRProfile(StructureDefinition profileDef, String url, String igName, String resourceType) {
        this.igName = igName;
        this.resourceType = resourceType;
        isAbstract = false;
        this.url = url;
        this.importsList = new HashSet<>();
    }

    public String getParentRef() {
        return parentRef;
    }

    public void setParentRef(String parentRef) {
        this.parentRef = parentRef;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract() {
        isAbstract = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamePrefix() {
        return CaseUtils.toCamelCase(igName, true, '_') + CaseUtils.toCamelCase(resourceType, true, '_');
    }

    /**
     * Returns a Ballerina-identifier-safe, collision-resistant suffix for this profile, used to name its
     * generated per-profile search-dispatch stub function ("search&lt;suffix&gt;"). Derived from the profile's
     * canonical URL (unique per FHIR profile by spec) rather than its bare StructureDefinition name: two
     * different profiles -- even across unrelated resource types -- can share the same name (e.g. duplicated
     * generic example profiles reused verbatim across resources in the raw international-base spec), which
     * previously produced colliding module-level "search&lt;Name&gt;" function declarations and failed
     * {@code bal build} with "redeclared symbol".
     */
    public String getSearchFunctionSuffix() {
        String basis = (this.url != null && !this.url.isEmpty()) ? this.url : this.name;
        String sanitized = basis.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
        if (sanitized.isEmpty() || !Character.isLetter(sanitized.charAt(0))) {
            sanitized = "p_" + sanitized;
        }
        return CaseUtils.toCamelCase(resourceType, true, '_') + "_" + sanitized;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public void setFhirVersion(String fhirVersion) {
        this.fhirVersion = fhirVersion;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<String> getImportsList() {
        return importsList;
    }

    public void addImport(String importItem) {
        this.importsList.add(importItem);
    }

    public void addExample(String interaction, JsonObject example) {
        examples.put(interaction, example);
    }

    public Map<String, JsonObject> getExamples() {
        return examples;
    }

    public String getIgName() {
        return igName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(BallerinaProjectToolConfig config) {
        if (config.isGenerateIgModuleEnabled() && config.getGenerateIgModuleName() != null
                && config.getGenerateIgModuleName().matches(MODULE_NAME_PATTERN)) {
            this.packagePrefix = config.getGenerateIgModuleName();
            return;
        }
        // Prefer this profile's own IG's resolved package over the single tool-wide default: in a multi-IG run
        // each IG maps to a different package, so falling back to the shared versionConfig value here would
        // give every profile the same (wrong, for all but one IG) package prefix.
        IncludedIGConfig igConfig = config.getIncludedIGConfigs().get(this.getIgName());
        String igPackage = (igConfig != null && igConfig.getImportStatement() != null)
                ? igConfig.getImportStatement() : config.getVersionConfig().getDependentPackage();
        if (igPackage.contains("/")) {
            String pkgNameWithoutOrg = igPackage.split("/")[1];
            this.packagePrefix = pkgNameWithoutOrg.substring(pkgNameWithoutOrg.lastIndexOf(".") + 1);
        } else {
            this.packagePrefix = igPackage.substring(igPackage.lastIndexOf(".") + 1);
        }
    }
}
