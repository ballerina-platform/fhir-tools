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
package org.wso2.healthcare.fhir.ballerina.packagegen.tool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Registry to store the data types defined for the package
 */
public class DataTypesRegistry {
    private static final DataTypesRegistry instance = new DataTypesRegistry();
    private final Set<String> dataTypesRegistry;

    private DataTypesRegistry() {
        dataTypesRegistry = new HashSet<>(Arrays.asList("boolean", "string", "decimal"));
    }

    public static DataTypesRegistry getInstance() {
        return instance;
    }

    public void addDataType(String dataType) {
        dataTypesRegistry.add(dataType);
    }

    public void removeDataType(String dataType) {
        dataTypesRegistry.remove(dataType);
    }

    public boolean containsDataType(String dataType) {
        return dataTypesRegistry.contains(dataType);
    }

    public Set<String> getDataTypesRegistry() {
        return dataTypesRegistry;
    }
}
