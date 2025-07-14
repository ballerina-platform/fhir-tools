/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.config.OperationConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated Ballerina service model that contains multiple FHIR APIs
 */
public class AggregatedService {
    private String name;
    private String fhirVersion;
    private List<String> importsList;
    private List<String> interceptorsList;
    private Map<String, BallerinaService> services;
    private List<OperationConfig> operationConfigs;
    private List<String> igs;

    public AggregatedService(String name, String fhirVersion) {
        this.name = name;
        this.fhirVersion = fhirVersion;
        importsList = new ArrayList<>();
        interceptorsList = new ArrayList<>();
        services = new HashMap<>();
        operationConfigs = new ArrayList<>();
        igs = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getNameLowerCase() {
        return name.toLowerCase();
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public void addImport(String importPackage) {
        if (!importsList.contains(importPackage)) {
            importsList.add(importPackage);
        }
    }

    public void addInterceptor(String interceptor) {
        interceptorsList.add(interceptor);
    }

    public void addService(String resourceType, BallerinaService service) {
        services.put(resourceType, service);
        // Merge imports from the service
        for (String importPackage : service.getImportsList()) {
            addImport(importPackage);
        }
        // Merge IGs
        for (String ig : service.getIgs()) {
            if (!igs.contains(ig)) {
                igs.add(ig);
            }
        }
    }

    public void setOperationConfigs(List<OperationConfig> operationConfigs) {
        this.operationConfigs = operationConfigs;
    }

    public List<String> getImportsList() {
        return importsList;
    }

    public List<String> getInterceptorsList() {
        return interceptorsList;
    }

    public Map<String, BallerinaService> getServices() {
        return services;
    }

    public List<OperationConfig> getOperationConfigs() {
        return operationConfigs;
    }

    public List<String> getIgs() {
        return igs;
    }

    public List<BallerinaService> getServicesList() {
        return new ArrayList<>(services.values());
    }
}
