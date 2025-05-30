/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.health.cmd.core.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import org.apache.commons.lang.StringUtils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class HealthCmdConfig {

    public static JsonObject getParsedConfigFromPath(Path toolConfigPath) throws BallerinaHealthException {
        Gson gson = new Gson();
        if (toolConfigPath != null && StringUtils.isNotBlank(toolConfigPath.toString())) {
            try {
                JsonObject toolConfigObj = gson.fromJson(new FileReader(toolConfigPath.toFile()), JsonObject.class);
                if (toolConfigObj != null) {
                    return toolConfigObj;
                }
            } catch (FileNotFoundException e) {
                throw new BallerinaHealthException("Error occurred while loading tool config file for the path: " +
                        toolConfigPath, e);
            }
        }
        return null;
    }

    public static JsonObject getParsedConfigFromStream(InputStream toolConfigFileStream) throws BallerinaHealthException {
        Gson gson = new Gson();
        if (toolConfigFileStream != null) {
            return gson.fromJson(new InputStreamReader(toolConfigFileStream), JsonObject.class);
        }
        return null;
    }
}
