package io.ballerina.health.cmd.core.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

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

    public static String generateIgNameFromPath(String specPath) throws BallerinaHealthException {
        if (specPath.contains(File.separator)) {
            //nested path given as input, last element is the IG name
            return specPath.substring(specPath.lastIndexOf(File.separator) + 1).replaceAll(
                    " ","-").replaceAll("\\$","-");
        }
        return specPath.replaceAll(" ","-").replaceAll("\\$","-");
    }

    public static JsonElement getIGConfigElement(String igName, String igCode, String specPath) throws BallerinaHealthException {

        JsonObject igConfig = new JsonObject();
        if (igName == null || igName.isEmpty()) {
            igName = generateIgNameFromPath(specPath);
        }
        igConfig.add("name", new Gson().toJsonTree(igName));

        if (igCode != null && igCode.isEmpty()) {
            igConfig.add("code", new Gson().toJsonTree(igCode));
        } else {
            igConfig.add("code", new Gson().toJsonTree(igName));
        }
        igConfig.add("dirPath", new Gson().toJsonTree(specPath));
        return igConfig;
    }

    public static void throwLauncherException(Throwable error) throws BLauncherException{
        BLauncherException launcherException = new BLauncherException();
        launcherException.initCause(error);
        throw launcherException;
    }
}
