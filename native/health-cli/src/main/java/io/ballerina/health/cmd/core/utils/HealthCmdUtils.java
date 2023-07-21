package io.ballerina.health.cmd.core.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.cli.launcher.BLauncherException;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for health command.
 */
public class HealthCmdUtils {
    /**
     * Exit with error code 1.
     *
     * @param exit Whether to exit or not.
     */
    public static void exitError(boolean exit) {
        if (exit) {
            Runtime.getRuntime().exit(1);
        }
    }

    /**
     * Get classpath in the runtime
     */
    public static String getClassPath() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        String allClassPath = bean.getClassPath();
        String[] classPath = allClassPath.split(":");
        for (String path : classPath) {
            if (path.contains(HealthCmdConstants.CMD_MVN_ARTIFACT_NAME)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Get resource path in runtime.
     */
    public static String getRuntimeResourcePath() {
        String classPath = getClassPath();
        String toolHome = null;
        if (classPath != null) {
            toolHome = classPath.substring(0, classPath.lastIndexOf("/tool/"));
        }
        return toolHome + HealthCmdConstants.CMD_RESOURCE_PATH_SUFFIX;
    }

    public static String generateIgNameFromPath(String specPath) {
        if (specPath.contains(File.separator)) {
            //nested path given as input, last element is the IG name
            return specPath.substring(specPath.lastIndexOf(File.separator) + 1).replaceAll(
                    " ", "-").replaceAll("\\$", "-");
        }
        return specPath.replaceAll(" ", "-").replaceAll("\\$", "-");
    }

    public static JsonElement getIGConfigElement(String igName, String igCode, String specPath) {

        JsonObject igConfig = new JsonObject();
        if (igName == null || igName.isEmpty()) {
            igName = generateIgNameFromPath(specPath);
        }
        igConfig.add("name", new Gson().toJsonTree(igName));
        igConfig.add("code", new Gson().toJsonTree(igCode));
        igConfig.add("dirPath", new Gson().toJsonTree(specPath));
        return igConfig;
    }

    public static String generateIgDirectoryPath(String specPath, String igName) {
        if (specPath.endsWith(File.separator)) {
            return igName + File.separator;
        }
        return File.separator + igName + File.separator;
    }

    public static JsonElement getIGConfigElement(String igName, String igCode) {

        if (igCode != null && igCode.isEmpty()) {
            return getIGConfigElement(igName, igCode, "");
        } else {
            return getIGConfigElement(igName, igName, "");
        }
    }

    public static void throwLauncherException(Throwable error) throws BLauncherException {
        BLauncherException launcherException = new BLauncherException();
        launcherException.initCause(error);
        throw launcherException;
    }

    public static List<String> getDirectories(Path specifiedPath) {
        List<String> subDirectories = new ArrayList<>();
        File specPath = new File(specifiedPath.toString());
        File[] files = specPath.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    subDirectories.add(file.getName());
                }
            }
        }
        return subDirectories;
    }
}
