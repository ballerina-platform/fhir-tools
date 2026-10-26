/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package io.ballerina.health.service.job;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * User supplied description of one generation run. Mirrors the option set of {@code bal health fhir}.
 */
public class GenerationRequest {

    /** Generation mode: {@code package} or {@code template}. */
    private String mode;

    /** Where the FHIR definitions come from. */
    private SpecSource source = new SpecSource();

    private String packageName;
    private String orgName;
    private String packageVersion;

    /** Published Ballerina package to import instead of embedding the IG (template mode). */
    private String dependentPackage;

    /** Module name for the embedded IG (template mode, ignored when dependentPackage is set). */
    private String igModuleName;

    private List<String> dependentIgs = new ArrayList<>();
    private List<String> includedProfiles = new ArrayList<>();
    private List<String> excludedProfiles = new ArrayList<>();

    /** Aggregated API mode (template mode). */
    private Boolean aggregate = Boolean.TRUE;

    /** FHIR resources to include in the aggregated API (template mode). */
    private List<String> resources = new ArrayList<>();

    /** Skip .choreo, OAS, .gitignore and Ballerina.toml (template mode). */
    private Boolean minimal = Boolean.FALSE;

    /** Optional human readable label shown in the UI job list. */
    private String label;

    /**
     * Origin of the FHIR definitions used for a run.
     */
    public static class SpecSource {

        /** {@code registry}, {@code upload} or {@code default}. */
        private String type = "registry";

        @SerializedName("igName")
        private String igName;

        @SerializedName("igVersion")
        private String igVersion;

        private String registryUrl;
        private Boolean forceDownload = Boolean.FALSE;

        /** Identifier of a previously uploaded specification bundle. */
        private String specId;

        /** FHIR version used to pick the default core IG when {@code type} is {@code default}. */
        private String fhirVersion = "r4";

        public String getType() {
            return type == null ? "registry" : type.trim().toLowerCase();
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getIgName() {
            return igName;
        }

        public void setIgName(String igName) {
            this.igName = igName;
        }

        public String getIgVersion() {
            return igVersion;
        }

        public void setIgVersion(String igVersion) {
            this.igVersion = igVersion;
        }

        public String getRegistryUrl() {
            return registryUrl;
        }

        public void setRegistryUrl(String registryUrl) {
            this.registryUrl = registryUrl;
        }

        public boolean isForceDownload() {
            return Boolean.TRUE.equals(forceDownload);
        }

        public void setForceDownload(Boolean forceDownload) {
            this.forceDownload = forceDownload;
        }

        public String getSpecId() {
            return specId;
        }

        public void setSpecId(String specId) {
            this.specId = specId;
        }

        public String getFhirVersion() {
            return fhirVersion == null || fhirVersion.isBlank() ? "r4" : fhirVersion.trim().toLowerCase();
        }

        public void setFhirVersion(String fhirVersion) {
            this.fhirVersion = fhirVersion;
        }
    }

    public String getMode() {
        return mode == null ? null : mode.trim().toLowerCase();
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public SpecSource getSource() {
        if (source == null) {
            source = new SpecSource();
        }
        return source;
    }

    public void setSource(SpecSource source) {
        this.source = source;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    public String getDependentPackage() {
        return dependentPackage;
    }

    public void setDependentPackage(String dependentPackage) {
        this.dependentPackage = dependentPackage;
    }

    public String getIgModuleName() {
        return igModuleName;
    }

    public void setIgModuleName(String igModuleName) {
        this.igModuleName = igModuleName;
    }

    public List<String> getDependentIgs() {
        return dependentIgs == null ? List.of() : dependentIgs;
    }

    public void setDependentIgs(List<String> dependentIgs) {
        this.dependentIgs = dependentIgs;
    }

    public List<String> getIncludedProfiles() {
        return includedProfiles == null ? List.of() : includedProfiles;
    }

    public void setIncludedProfiles(List<String> includedProfiles) {
        this.includedProfiles = includedProfiles;
    }

    public List<String> getExcludedProfiles() {
        return excludedProfiles == null ? List.of() : excludedProfiles;
    }

    public void setExcludedProfiles(List<String> excludedProfiles) {
        this.excludedProfiles = excludedProfiles;
    }

    public boolean isAggregate() {
        return !Boolean.FALSE.equals(aggregate);
    }

    public void setAggregate(Boolean aggregate) {
        this.aggregate = aggregate;
    }

    public List<String> getResources() {
        return resources == null ? List.of() : resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public boolean isMinimal() {
        return Boolean.TRUE.equals(minimal);
    }

    public void setMinimal(Boolean minimal) {
        this.minimal = minimal;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
