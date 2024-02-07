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

package io.ballerina.health.cmd.core.utils;

/**
 * This class contains the messages constants required for Health tool.
 */
public class ErrorMessages {

    public static final String CONFIG_INITIALIZING_FAILED = "Error while initializing tool config. Please check the " +
            "config file and try again";
    public static final String CONFIG_ACCESS_FAILED = "Error while accessing tool config.";
    public static final String TOOL_EXECUTION_FAILED = "Error while executing tool.";
    public static final String CONFIG_PARSE_ERROR = "Error while parsing the config.";
    public static final String LIB_INITIALIZING_FAILED = "Error while initializing protocol lib configs. Please check the " +
            "config file and try again";

    public static final String UNKNOWN_ERROR = "An error occurred. Please check the arguments and retry. If the " +
            "issue keep occurring contact tool developers";

    public static final String INVALID_MODE = "Invalid mode, Please check the mode and retry";

    public static final String INVALID_OPTION_PROVIDED = "Invalid option provided, Please check the option and retry";

    public static final String TOOL_IMPL_NOT_FOUND = "Tool implementation not found for the given tool. Please check " +
            "the distribution libs and try again";

    public static final String ARG_VALIDATION_FAILED = "Argument validation failed. Please check the arguments and " +
            "try again";

    private ErrorMessages() {
        throw new AssertionError();
    }
}
