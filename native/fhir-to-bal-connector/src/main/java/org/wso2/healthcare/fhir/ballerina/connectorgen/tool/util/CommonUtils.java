package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.util;

import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CommonUtils {

    public static String generateFilePath(String rootPath, String project, String fileName) {
        if (fileName.isEmpty()) {
            return rootPath + File.separator + project;
        }
        return rootPath + File.separator + project + File.separator + fileName;
    }

    public static void copyResourceDir(URL resourceUrl, Path targetDir) throws IOException, URISyntaxException {
        // Ensure destination exists
        Files.createDirectories(targetDir);

        if (resourceUrl == null) {
            throw new IOException("Resource not found: " + Constants.BALLERINA_CONNECTOR_TOOL);
        }

        if (resourceUrl.getProtocol().equals("jar")) {
            // Resource is inside a JAR
            String jarPath = resourceUrl.getPath().substring(5, resourceUrl.getPath().indexOf("!"));
            try (JarFile jar = new JarFile(jarPath)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(Constants.BALLERINA_CONNECTOR_TOOL + "/")) {
                        Path dest = targetDir.resolve(name.substring(Constants.BALLERINA_CONNECTOR_TOOL.length() + 1));
                        if (entry.isDirectory()) {
                            Files.createDirectories(dest);
                        } else {
                            Files.createDirectories(dest.getParent());
                            try (InputStream is = jar.getInputStream(entry)) {
                                Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            }
        } else {
            // Resource is on the filesystem (e.g., running in IDE)
            Path sourcePath = Paths.get(resourceUrl.toURI());
            Files.walk(sourcePath).forEach(source -> {
                Path dest = targetDir.resolve(sourcePath.relativize(source).toString());
                try {
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
