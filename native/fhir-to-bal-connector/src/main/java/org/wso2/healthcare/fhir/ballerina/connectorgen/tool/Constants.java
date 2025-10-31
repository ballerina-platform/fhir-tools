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

package org.wso2.healthcare.fhir.ballerina.connectorgen.tool;

import java.time.LocalDate;
import java.util.List;

public class Constants {
    public static final String TYPE = "type";
    public static final String INTERACTIONS = "interactions";
    public static final String OPERATIONS = "operations";
    public static final String SEARCH_PARAMS = "searchParams";
    public static final String ORIGINAL_NAME = "originalName";
    public static final String RESOLVED_NAME = "resolvedName";
    public static final String DOCUMENTATION = "documentation";
    public static final String SUPPORTED_PROFILES = "supportedProfiles";
    public static final String TYPE_STRING = "typeString";
    public static final String PROFILE_PACKAGES = "profilePackages";
    public static final String INTERNATIONAL_PACKAGE = "internationalPackage";
    public static final String RESOURCES = "resources";
    public static final String RESOURCE_INTERNATIONAL = "resourceInternational";
    public static final String BALLERINA_CONNECTOR_TOOL = "ballerina-connector-tool";

    public static final String BASE_PACKAGE = "health.fhir.r4.international401";

    public static final List<String> SKIP_LIST = List.of(
            "Bundle",
            "CodeSystem",
            "OperationOutcome",
            "ValueSet"
    );

    public static final List<String> KNOWN_GET_OPERATIONS = List.of(
            "find",
            "everything",
            "meta",
            "versions",
            "data-requirements"
    );

    public static final String INTERACTIONS_CREATE = "create";
    public static final String INTERACTIONS_UPDATE = "update";
    public static final String INTERACTIONS_PATCH = "patch";

    public static final String LICENSE_YEAR = String.valueOf(LocalDate.now().getYear());
}
