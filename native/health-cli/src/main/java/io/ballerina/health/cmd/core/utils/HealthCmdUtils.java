package io.ballerina.health.cmd.core.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    public static String generateCustomIGPath(String igName) throws BallerinaHealthException {
        if (igName.isEmpty() || igName.contains(" ")) {
            throw new BallerinaHealthException("Invalid IG name: " + igName + ". IG name cannot be empty or contain " +
                    "spaces.");
        }
        return File.separator + "profiles" + File.separator + igName + File.separator;
    }

    public static JsonElement getIGConfigElement(String igName, String igCode) throws BallerinaHealthException {

        JsonObject igConfig = new JsonObject();
        igConfig.add("name", new Gson().toJsonTree(igName));

        if (igCode != null && igCode.isEmpty()) {
            igConfig.add("code", new Gson().toJsonTree(igCode));
        } else {
            igConfig.add("code", new Gson().toJsonTree(igName));
        }
        igConfig.add("dirPath", new Gson().toJsonTree(generateCustomIGPath(igName)));
        return igConfig;
    }
}
