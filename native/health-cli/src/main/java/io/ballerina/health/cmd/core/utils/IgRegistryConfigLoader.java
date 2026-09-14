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

package io.ballerina.health.cmd.core.utils;

import com.google.gson.JsonObject;
import io.ballerina.health.cmd.core.config.HealthCmdConfig;
import io.ballerina.health.cmd.core.config.IgRegistryConfig;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads {@link IgRegistryConfig} from the packaged tool-config.json.
 */
public final class IgRegistryConfigLoader {

    private IgRegistryConfigLoader() {
    }

    public static IgRegistryConfig load() {
        try (InputStream stream = IgRegistryConfigLoader.class.getClassLoader()
                .getResourceAsStream(HealthCmdConstants.CMD_CONFIG_FILENAME)) {
            if (stream == null) {
                return IgRegistryConfig.defaults();
            }
            JsonObject config = HealthCmdConfig.getParsedConfigFromStream(stream);
            if (config != null && config.has("fhir")) {
                return IgRegistryConfig.fromFhirConfig(config.getAsJsonObject("fhir"));
            }
        } catch (BallerinaHealthException | IOException | RuntimeException ignored) {
            // Malformed igRegistry config (e.g. unexpected JSON types/shape) falls back to defaults.
        }
        return IgRegistryConfig.defaults();
    }
}
