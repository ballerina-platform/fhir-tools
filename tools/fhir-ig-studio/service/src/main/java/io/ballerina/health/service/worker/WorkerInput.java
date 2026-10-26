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

package io.ballerina.health.service.worker;

import java.util.List;

/**
 * Fully resolved instruction handed to a forked {@link GenerationWorker} JVM. Everything the worker needs is
 * captured here so the worker performs no policy decisions of its own.
 */
public class WorkerInput {

    /** {@code package} or {@code template}. */
    public String mode;

    /** Working directory for the worker; downloaded IGs land under {@code <executionPath>/spec}. */
    public String executionPath;

    /** Where generated artifacts are written. */
    public String outputPath;

    /** Explicit local specification directory, or {@code null} to resolve from the registry. */
    public String specPath;

    /** Concrete registry package coordinates, or {@code null} for the configured default core IG. */
    /** What to call the guide in generated documentation. Descriptive only; never used to resolve anything. */
    public String igLabel;
    public String igName;
    public String igVersion;
    public String registryUrl;
    public String igCacheDir;
    public boolean forceDownload;

    /** Selects the default core IG when {@link #igName} and {@link #specPath} are both absent. */
    public String fhirVersion;

    public String packageName;
    public String orgName;
    public String packageVersion;
    public String dependentPackage;
    public boolean explicitDependentPackage;
    public String igModuleName;
    public List<String> dependentIgs;
    public List<String> includedProfiles;
    public List<String> excludedProfiles;
    public boolean aggregate = true;
    public String resources;
    public boolean minimal;
}
