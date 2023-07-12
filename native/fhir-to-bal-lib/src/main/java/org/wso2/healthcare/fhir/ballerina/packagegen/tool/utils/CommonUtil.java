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

import org.apache.commons.io.FileUtils;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Common utility functions of Package Gen Tool.
 */
public class CommonUtil {


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
        return newCode.replaceAll(" *\\(.+?\\)", "").replace("+", "");
    }

    /**
     * Copy contents in a directory to another.
     *
     * @param sourcePath      source directory
     * @param destinationPath destination directory
     */
    public static void copyContentsToDir(String sourcePath, String destinationPath) throws CodeGenException {
        try {
            File source = new File(sourcePath);
            File dest = new File(destinationPath);
            FileUtils.copyDirectory(source, dest);
        } catch (IOException e) {
            throw new CodeGenException(
                    "Error occurred while copying contents from " + sourcePath + " to " + destinationPath, e);
        }
    }

    /**
     * Copy contents of JAR's resources path to another directory.
     * @param sourceJarPath
     * @param destinationPath
     * @throws IOException
     */
    public static void copyContentFormJar(String sourceJarPath, String destinationPath) throws IOException {
        File destinationFolder = new File(destinationPath);
        destinationFolder.mkdirs(); // Create the directory if it doesn't exist

        JarFile jarFile = new JarFile(sourceJarPath);

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            // Get the name of the entry (relative path within the JAR file)
            String entryName = entry.getName();

            // Create a file for the corresponding entry in the destination folder
            File destFile = new File(destinationFolder, entryName);

            if (entry.isDirectory()) {
                // Create the directory in the destination folder
                destFile.mkdirs();
            } else {
                // Create the file in the destination folder and copy the contents
                InputStream inputStream = jarFile.getInputStream(entry);
                OutputStream outputStream = Files.newOutputStream(destFile.toPath());

                // Copy the contents from the JAR entry to the destination file
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // Close the streams
                outputStream.close();
                inputStream.close();
            }
        }
        // Close the JAR file
        jarFile.close();
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
                    words += "-" + unitsMap[number % 10];
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
}
