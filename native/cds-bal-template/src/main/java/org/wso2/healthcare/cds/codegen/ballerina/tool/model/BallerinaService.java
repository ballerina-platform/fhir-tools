/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.cds.codegen.ballerina.tool.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Ballerina service model
 */
public class BallerinaService {
    private String name;
    private Map<String, CdsHook> cdsHooks = new HashMap<>();

    public BallerinaService() {
    }

    public BallerinaService(String name, Map<String, CdsHook> cdsHooks) {
        this.name = name;
        this.cdsHooks = cdsHooks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, CdsHook> getCdsHooks() {
        return cdsHooks;
    }

    public void setCdsHooks(Map<String, CdsHook> cdsHooks) {
        this.cdsHooks = cdsHooks;
    }
}
