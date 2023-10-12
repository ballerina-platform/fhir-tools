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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool;

/**
 * Constants for Ballerina Project Gen tool
 */
public class BallerinaProjectConstants {
    public static final String CONFIG_ENABLE = "enable";
    public static final String CONFIG_PROFILE_IG = "implementationGuide";
    public static final String RESOURCE_PATH_TEMPLATES = "template";
    public static final String PROJECT_API_SUFFIX = ".api";
    public static final String BASE_PACKAGE_IMPORT_SUFFIX = "ballerinax/health.fhir.";
    public static final String SERVICE_PACKAGE_IMPORT_SUFFIX = "ballerinax/health.fhir";
    public static final String INTERNATIONAL_PACKAGE_IMPORT_SUFFIX = "ballerinax/health.fhir.r4.international401";

    public static final String READ_METHOD_DESC = "Read the current state of single resource based on its id.";
    public static final String VREAD_METHOD_DESC = "Read the state of a specific version of a resource based on its id.";
    public static final String SEARCH_TYPE_METHOD_DESC = "Search for resources based on a set of criteria.";
    public static final String CREATE_METHOD_DESC = "Create a new resource.";
    public static final String UPDATE_METHOD_DESC = "Update the current state of a resource completely.";
    public static final String PATCH_METHOD_DESC = "Update the current state of a resource partially.";
    public static final String DELETE_METHOD_DESC = "Delete a resource.";
    public static final String HISTORY_INSTANCE_METHOD_DESC = "Retrieve the update history for a particular resource.";
    public static final String HISTORY_TYPE_METHOD_DESC = "Retrieve the update history for all resources.";
}
