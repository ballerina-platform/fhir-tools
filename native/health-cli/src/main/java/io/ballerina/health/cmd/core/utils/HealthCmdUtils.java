package io.ballerina.health.cmd.core.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import net.consensys.cava.toml.Toml;
import net.consensys.cava.toml.TomlArray;
import net.consensys.cava.toml.TomlParseResult;
import net.consensys.cava.toml.TomlTable;
import org.wso2.healthcare.codegen.tool.framework.commons.core.TemplateGenerator;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    public static Path validateAndSetSpecificationPath(String specPathParam, String executionPath) throws BallerinaHealthException {
        Path specificationPath = getSpecificationPath(specPathParam, executionPath);
        if (!Files.isDirectory(specificationPath)) {
            throw new BallerinaHealthException("Cannot find valid spec path pointed. Please check the path "
                    + specPathParam + " is valid.");
        }
        return specificationPath;
    }

    public static Path getSpecificationPath(String specPathParam, String executionPath) throws BallerinaHealthException {
        if (specPathParam == null && specPathParam.isEmpty()) {
            throw new BallerinaHealthException("Cannot find valid spec path pointed. Please check the path "
                    + specPathParam + " is valid.");
        }

        Path specificationPath;
        Path path = Paths.get(specPathParam);
        if (path.isAbsolute()) {
            specificationPath = path;
        } else {
            specificationPath = Paths.get(executionPath, specPathParam);
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
            throw new BallerinaHealthException(fileName + " is not found");
        }
        return ioStream;
    }

    public static JsonObject parseTomlToJson(String path) {
        TomlParseResult parseResult = null;
        try {
            parseResult = Toml.parse(Paths.get(path));
        } catch (IOException e) {
            throwLauncherException(e);
        }

        return tomlToJson(parseResult);
    }

    public static JsonObject tomlToJson(TomlParseResult tomlParseResult) {
        String josnString;
        if (tomlParseResult instanceof TomlArray) {
            josnString = ((TomlArray) tomlParseResult).toJson();

        } else if (tomlParseResult instanceof TomlTable) {
            josnString = ((TomlTable) tomlParseResult).toJson();
        } else {
            josnString = tomlParseResult.toJson();
        }

        return JsonParser.parseString(josnString).getAsJsonObject();
    }

    public static String getSpecFhirVersion(String specificationPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(specificationPath))) {
            List<Path> jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .toList();

            Path implGuide = jsonFiles.stream()
                    .filter(path -> path.getFileName().toString().startsWith("ImplementationGuide"))
                    .filter(path -> {
                        try{
                            String content = Files.readString(path);
                            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                            return json.has("resourceType") &&
                                    json.get("resourceType").getAsString().equals("ImplementationGuide") &&
                                    json.has("fhirVersion");
                        }
                        catch (Exception e){
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(null);

            Path structureDefinition = jsonFiles.stream()
                    .filter(path -> path.getFileName().toString().startsWith("StructureDefinition"))
                    .filter(path -> {
                        try{
                            String content = Files.readString(path);
                            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                            return json.has("resourceType") &&
                                    json.get("resourceType").getAsString().equals("StructureDefinition") &&
                                    json.has("fhirVersion");
                        }
                        catch (Exception e){
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(null);

            Path target = implGuide != null ? implGuide : structureDefinition;
            return target != null ? HealthCmdUtils.extractFhirVersion(target) : null;
        }
    }

    private static String extractFhirVersion(Path path) {
        String fhirVersion = "";

        try {
            String content = Files.readString(path);
            if (path.toString().endsWith(".json")) {
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                fhirVersion = json.has("fhirVersion") ? json.get("fhirVersion").getAsString() : null;
            }

            if (fhirVersion.startsWith("4.")) {
                fhirVersion = "r4";
            } else if (fhirVersion.startsWith("5.")) {
                fhirVersion = "r5";
            }

            return fhirVersion;
        } catch (NullPointerException e) {
            System.err.println("FHIR version not found in the file: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading FHIR version from file: " + e.getMessage());
        }
        return null;
    }
}
