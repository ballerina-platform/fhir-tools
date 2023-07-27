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

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils;

import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common utility functions of Package Gen Tool.
 */
public class CommonUtil {

    private static final Pattern separatedIdentifierPattern = Pattern.compile("^[a-zA-Z0-9_.]*$");
    private static final Pattern onlyDotsPattern = Pattern.compile("^[.]+$");
    private static final Pattern onlyNonAlphanumericPattern = Pattern.compile("^[^a-zA-Z0-9]+$");


    /**
     * Concat path segments with separator.
     *
     * @param rootPath root path
     * @param project  project name
     * @param fileName file name
     * @return generated file path
     */
    public static String generateFilePath(String rootPath, String project, String fileName) {

        if (fileName.isEmpty()) {
            return rootPath + File.separator + project;
        }
        return rootPath + File.separator + project + File.separator + fileName;
    }


    /**
     * Creates nested directory structure.
     *
     * @param nestedPath nested path to be created
     * @throws CodeGenException if an error occurs while creating nested directory
     */
    public static void createNestedDirectory(String nestedPath) throws CodeGenException {

        Path pathFoSubFolder = Paths.get(nestedPath);
        try {
            Files.createDirectories(pathFoSubFolder);
        } catch (IOException e) {
            throw new CodeGenException("Error occurred while creating nested directory on path: " + nestedPath, e);
        }
    }

    /**
     * Returns the preferred token after string split.
     *
     * @param string        string to be split
     * @param delimiter     delimiter for split
     * @param tokenPosition position of the preferred token
     * @return preferred token
     */
    public static String getSplitTokenAt(String string, String delimiter, ToolConstants.TokenPosition tokenPosition) {
        String[] tokens = string.split(delimiter);
        int position = 0;

        if (tokens.length > 0) {
            switch (tokenPosition) {
                case BEGIN:
                    break;
                case MIDDLE:
                    position = tokens.length / 2;
                    break;
                case END:
                    position = tokens.length - 1;
                    break;
            }
        } else {
            return string;
        }
        return tokens[position];
    }

    /**
     * Parse multiline strings in to oneliner.
     *
     * @param str multiline strings
     * @return string oneliner
     */
    public static String parseMultilineString(String str) {
        String description = str.replaceAll("\\s{2,}", " ");
        description = description.replace("\n", " ").replace("\r", " ");
        return description.replace("\"", "'");
    }

    /**
     * Transform string to camelcase.
     *
     * @param str string
     * @return camelCased string
     */
    public static String toCamelCase(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Validate codes without special characters.
     *
     * @param code code
     * @return validated code
     */
    public static String validateCode(String code) {
        String newCode = code.trim().split(Pattern.quote(" "))[0];
        return newCode.replaceAll(" *\\(.+?\\)", "")
                .replace("+", "");
    }

    /**
     * Convert number to words.
     *
     * @param number number
     * @return words
     */
    public static String toWords(int number) {
        if (number == 0) {
            return "zero";
        }

        String words = "";
        if ((number / 1000000) > 0) {
            words += toWords(number / 1000000) + " million ";
            number %= 1000000;
        }

        if ((number / 1000) > 0) {
            words += toWords(number / 1000) + " thousand ";
            number %= 1000;
        }

        if ((number / 100) > 0) {
            words += toWords(number / 100) + " hundred ";
            number %= 100;
        }

        if (number > 0) {
            if (!words.equals("")) {
                words += "and ";
            }

            String[] unitsMap = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
                    "nineteen" };
            String[] tensMap = { "zero", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty",
                    "ninety" };

            if (number < 20) {
                words += unitsMap[number];
            } else {
                words += tensMap[number / 10];
                if ((number % 10) > 0) {
                    words += unitsMap[number % 10];
                }
            }
        }
        return words;
    }

    /**
     * Convert camel case to snake case.
     *
     * @param str string
     * @return snake case string
     */
    public static String camelToSnake(String str) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        str = str.replaceAll(regex, replacement).toLowerCase();
        return str;
    }

    /**
     * Guess package name with valid pattern.
     * Reference: <a href="https://github.com/ballerina-platform/ballerina-lang/blob/master/compiler/ballerina-lang/src/main/java/io/ballerina/projects/util/ProjectUtils.java">...</a>
     *
     * @param packageName package name
     * @return package name
     */
    public static String validateAndRevisePackageName(String packageName) {
        if (!validateOnlyNonAlphanumeric(packageName)) {
            packageName = "fhir_package";
        }

        if (!validatePackageName(packageName)) {
            packageName = packageName.replaceAll("[^a-zA-Z0-9_.]", "_");
        }

        // if package name is starting with numeric character, prepend `pkg`
        if (packageName.matches("[0-9].*")) {
            packageName = "pkg" + packageName;
        }

        // if package name is starting with underscore remove it
        if (packageName.startsWith("_")) {
            packageName = removeFirstChar(packageName);
        }

        // if package name has consecutive underscores, replace them with a single underscore
        if (packageName.contains("__")) {
            packageName = packageName.replaceAll("__", "_");
        }

        // if package name has trailing underscore remove it
        if (packageName.endsWith("_")) {
            packageName = removeLastChar(packageName);
        }
        return packageName;
    }

    private static boolean validateOnlyNonAlphanumeric(String identifiers) {
        Matcher m = onlyNonAlphanumericPattern.matcher(identifiers);

        return !m.matches();
    }

    /**
     * Validates the package name.
     *
     * @param packageName The package name.
     * @return True if valid package name, else false.
     */
    public static boolean validatePackageName(String packageName) {
        return validateDotSeparatedIdentifiers(packageName)
                && validateUnderscoresOfName(packageName)
                && validateInitialNumericsOfName(packageName);
    }

    private static boolean validateDotSeparatedIdentifiers(String identifiers) {
        Matcher m = separatedIdentifierPattern.matcher(identifiers);
        Matcher mm = onlyDotsPattern.matcher(identifiers);

        return m.matches() && !mm.matches();
    }

    /**
     * Checks the organization, package or module name has initial, trailing or consecutive underscores.
     *
     * @param name name.
     * @return true if name does not have initial, trailing or consecutive underscores, else false.
     */
    public static boolean validateUnderscoresOfName(String name) {
        return !(name.startsWith("_") || name.endsWith("_") || name.contains("__"));
    }

    /**
     * Checks the organization, package or module name has initial numeric characters.
     *
     * @param name name.
     * @return true if name does not have initial numeric characters, else false.
     */
    public static boolean validateInitialNumericsOfName(String name) {
        return !name.matches("[0-9].*");
    }

    /**
     * Remove first character of the given string.
     *
     * @param aString given string
     * @return string removed last character
     */
    public static String removeFirstChar(String aString) {
        return aString.substring(1);
    }

    /**
     * Remove last character of the given string.
     *
     * @param aString given string
     * @return string removed last character
     */
    public static String removeLastChar(String aString) {
        return aString.substring(0, aString.length() - 1);
    }
}
