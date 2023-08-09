package io.ballerina.health.cmd.core.utils;

import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static void throwLauncherException(Throwable error) throws BLauncherException {
        BLauncherException launcherException = new BLauncherException();
        launcherException.initCause(error);
        throw launcherException;
    }

    public static List<String> getDirectories(String specifiedPath) {
        List<String> subDirectories = new ArrayList<>();
        File specPath = new File(specifiedPath);
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

    public static Path validateAndSetSpecificationPath(String specPathParam,String executionPath) throws BallerinaHealthException {

        Path specificationPath = null;
        if (specPathParam != null) {
            Path path = Paths.get(specPathParam);
            if (path.isAbsolute()) {
                specificationPath =  path;
            } else {
                specificationPath = Paths.get(executionPath, specPathParam);
            }
            if (!Files.isDirectory(specificationPath)) {
                throw new BallerinaHealthException("Cannot find valid spec path pointed. Please check the path "
                        + specPathParam + " is valid.");
            }
        }
        return specificationPath;
    }

    public static void engageChildTemplateGenerators(TemplateGenerator templateGenerator, ToolContext context,
                                               Map<String, Object> properties) throws CodeGenException {
        if (templateGenerator != null) {
            templateGenerator.generate(context, properties);
            engageChildTemplateGenerators(templateGenerator.getChildTemplateGenerator(), context, properties);
        }
    }

    public static InputStream getResourceFile(Class<?> handlerClass, String fileName) throws BallerinaHealthException {
        ClassLoader classLoader = handlerClass.getClassLoader();
        InputStream ioStream = classLoader.getResourceAsStream(fileName);

        if (ioStream == null) {
            throw new BallerinaHealthException(HealthCmdConstants.CMD_CONFIG_FILENAME + " is not found");
        }
        return ioStream;
    }
}
