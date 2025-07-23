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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.cli.cmd.CommandUtil;
import io.ballerina.projects.util.ProjectUtils;
import net.consensys.cava.toml.TomlArray;
import net.consensys.cava.toml.TomlTable;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ballerina package level config.
 */
public class PackageConfig {
    private String org;
    private String name;
    private String version;
    private String ballerinaDistribution;
    private String authors;
    private String fhirVersion;
    private String repository;
    private String basePackage;
    private String internationalPackage;
    private List<DependencyConfig> dependencyConfigList;
    private final Map<String, String> dependentIgs = new HashMap<>();

    public PackageConfig(JsonObject packageConfigJson) {
        this.org = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_ORG).getAsString();
        this.name = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_NAME).getAsString();
        this.version = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_VERSION).getAsString();
        this.ballerinaDistribution = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_DISTRIBUTION).getAsString();
        this.authors = packageConfigJson.getAsJsonArray(ToolConstants.CONFIG_PACKAGE_AUTHORS).getAsString();
        this.fhirVersion = packageConfigJson.getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_FHIR_VERSION).getAsString();

        this.repository = packageConfigJson.
                getAsJsonObject(ToolConstants.CONFIG_VERSION_CONFIGS).
                getAsJsonObject(this.fhirVersion).
                getAsJsonPrimitive(ToolConstants.CONFIG_PACKAGE_REPOSITORY).getAsString();

        this.basePackage = packageConfigJson.
                getAsJsonObject(ToolConstants.CONFIG_VERSION_CONFIGS).
                getAsJsonObject(this.fhirVersion).
                getAsJsonPrimitive(ToolConstants.CONFIG_BASE_PACKAGE).getAsString();

        this.internationalPackage = packageConfigJson.
                getAsJsonObject(ToolConstants.CONFIG_VERSION_CONFIGS).
                getAsJsonObject(this.fhirVersion).
                getAsJsonPrimitive(ToolConstants.CONFIG_INTERNATIONAL_PACKAGE).getAsString();

        populateDependencies(packageConfigJson.getAsJsonArray(ToolConstants.CONFIG_PACKAGE_DEPENDENCY).getAsJsonArray());
    }

    public PackageConfig(TomlTable packageConfigToml) {
        this.org = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_ORG_TOML);
        this.name = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_NAME_TOML);
        this.version = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_VERSION_TOML);
        this.ballerinaDistribution = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_DISTRIBUTION_TOML);
        this.authors = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_AUTHORS_TOML);
        this.fhirVersion = packageConfigToml.getString(ToolConstants.CONFIG_PACKAGE_FHIR_VERSION_TOML);

        ///  NOTE: Check for the breakability of elements like the json config like above
        ///  when initiating the tool from a tool-config.toml file
        if (this.fhirVersion.equalsIgnoreCase("r4")) {
            this.repository = packageConfigToml.getString(ToolConstants.CONFIG_R4_PACKAGE_REPOSITORY_TOML);
            this.basePackage = packageConfigToml.getString(ToolConstants.CONFIG_R4_BASE_PACKAGE_TOML);
            this.internationalPackage = packageConfigToml.getString(ToolConstants.CONFIG_R4_INTERNATIONAL_PACKAGE_TOML);
        } else if (this.fhirVersion.equalsIgnoreCase("r5")) {
            this.repository = packageConfigToml.getString(ToolConstants.CONFIG_R5_PACKAGE_REPOSITORY_TOML);
            this.basePackage = packageConfigToml.getString(ToolConstants.CONFIG_R5_BASE_PACKAGE_TOML);
            this.internationalPackage = packageConfigToml.getString(ToolConstants.CONFIG_R5_INTERNATIONAL_PACKAGE_TOML);
        }

        populateDependencies(packageConfigToml.getArrayOrEmpty(ToolConstants.CONFIG_PACKAGE_DEPENDENCY_TOML));
    }

    private void populateDependencies(JsonArray dependencyArray) {
        dependencyConfigList = new ArrayList<>();
        for (int i = 0; i < dependencyArray.size(); i++) {
            dependencyConfigList.add(new DependencyConfig(dependencyArray.get(i).getAsJsonObject()));
        }
    }

    private void populateDependencies(TomlArray dependencyArray) {
        dependencyConfigList = new ArrayList<>();
        for (int i = 0; i < dependencyArray.size(); i++) {
            TomlTable item = dependencyArray.getTable(i);
            if (item != null) {
                dependencyConfigList.add(new DependencyConfig(item));
            }
        }
    }

    public String getOrg() {
        return org;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getBallerinaDistribution() {
        return ballerinaDistribution;
    }

    public String getAuthors() {
        return authors;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public String getRepository() {
        return repository;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public String getInternationalPackage() {
        return internationalPackage;
    }

    public List<DependencyConfig> getDependencyConfigList() {
        return dependencyConfigList;
    }

    public void setOrg(String org) {
        // org name of a ballerina package can have only alhpa-numeric chars and '_'
        // it cannot have consecutive '_'
        // and it cannot start/end with '_'
        String normalizedOrg = org.replaceAll("[^a-zA-Z0-9_]", "_");
        if (normalizedOrg.contains("__")) {
            normalizedOrg = normalizedOrg.replaceAll("_{2,}", "_");
        }
        if (normalizedOrg.startsWith("_")) {
            normalizedOrg = ProjectUtils.removeFirstChar(normalizedOrg);
        }
        if (normalizedOrg.endsWith("_")) {
            normalizedOrg = ProjectUtils.removeLastChar(normalizedOrg);
        }
        this.org = normalizedOrg;
    }

    public void setName(String name) {
        //reusing the Util from Ballerina-lang to validate the package name.
        //second argument is not valid for health packages.
        this.name = ProjectUtils.guessPkgName(name, "package");
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setBallerinaDistribution(String ballerinaDistribution) {
        this.ballerinaDistribution = ballerinaDistribution;
    }

    public void setFhirVersion(String fhirVersion) {
        this.fhirVersion = fhirVersion;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setInternationalPackage(String internationalPackage) {
        this.internationalPackage = internationalPackage;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public void setDependencyConfigList(List<DependencyConfig> dependencyConfigList) {
        this.dependencyConfigList = dependencyConfigList;
    }

    public Map<String, String> getDependentIgs() {
        return dependentIgs;
    }

    public void setDependentIgs(List<String> dependentIgs) {
        for (String dependentIg : dependentIgs) {
            // This regex will validate the profile dependentIg pattern <profile_url>=<Ballerina_org_name>/<Ballerina_package_name>
            Pattern pattern = Pattern.compile(ToolConstants.REGEX_FOR_DEPENDENT_IG_AND_PACKAGE);
            Matcher matcher = pattern.matcher(dependentIg);
            if (matcher.matches()) {
                String[] separatedIgUrlAndDependentPackage = dependentIg.split(ToolConstants.EQUAL_SIGN);
                this.dependentIgs.put(separatedIgUrlAndDependentPackage[0], separatedIgUrlAndDependentPackage[1]);
            } else {
                String errorMsg = "The provided dependent IG is incorrect: " + dependentIg + " \n" +
                        "It should be in the <IG_base_url>=<Ballerina_org>/<Ballerina_package_name> pattern, " +
                        "e-g: http://hl7.org/fhir/uv/=ballerinax/health.fhir.uv \n";

                System.out.println(errorMsg);
                CommandUtil.exitError(true);
            }
        }
    }
}
