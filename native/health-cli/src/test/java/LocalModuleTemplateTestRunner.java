/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import io.ballerina.health.cmd.handler.Handler;
import io.ballerina.health.cmd.handler.HandlerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Runner that validates default embedded IG module template generation.
 */
public class LocalModuleTemplateTestRunner {

    private static final Path EXECUTION_PATH = Paths.get(System.getProperty("user.dir"));

    public static void main(String[] args) throws Exception {
        runLocalModuleTemplateGenerationTest();
    }

    private static void runLocalModuleTemplateGenerationTest() throws URISyntaxException, BallerinaHealthException {
        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("--package-name", "uscore501");
        argsMap.put("--org-name", "ballerinax");
        argsMap.put("--package-version", "1.1.0");
        argsMap.put("--included-profile", null);
        argsMap.put("--excluded-profile", null);
        argsMap.put("--explicit-dependent-package", false);
        argsMap.put("--dependent-package", null);
        argsMap.put("--ig-module-name", "localprofile");
        argsMap.put("--dependent-ig", null);
        argsMap.put("--aggregate", true);
        argsMap.put("--resources", "Patient");
        argsMap.put("--minimal", false);

        String mode = "template";
        String command = "fhir";

        String resourcePath = Paths.get(
                Objects.requireNonNull(LocalModuleTemplateTestRunner.class.getClassLoader().getResource("io")).toURI()
        ).getParent().getParent().toString() + File.separator + "test-classes" + File.separator + "profiles.USCore";
        Path specificationPath = HealthCmdUtils.validateAndSetSpecificationPath(resourcePath, EXECUTION_PATH.toString());
        Path outputPath = Paths.get(resourcePath).getParent().resolve("local-module-template-test");
        cleanOutputDirectory(outputPath);

        Handler toolHandler;
        try {
            toolHandler = HandlerFactory.createHandler(command, mode, System.out, specificationPath.toString());
        } catch (BallerinaHealthException e) {
            throw new BLauncherException();
        }
        toolHandler.setArgs(argsMap);
        boolean executionStatus = toolHandler.execute(specificationPath.toString(), outputPath.toString());
        if (!executionStatus) {
            throw new RuntimeException("Local module template generation failed");
        }

        Path servicePath = outputPath.resolve("fhir-service").resolve("service.bal");
        Path localModuleInitializerPath = outputPath.resolve("fhir-service")
                .resolve("modules")
                .resolve("localprofile")
                .resolve("initializer.bal");

        if (!Files.exists(servicePath)) {
            throw new RuntimeException("Generated aggregated service not found: " + servicePath);
        }
        if (!Files.exists(localModuleInitializerPath)) {
            throw new RuntimeException("Generated IG module not found: " + localModuleInitializerPath);
        }

        try {
            String serviceContent = Files.readString(servicePath);
            if (!serviceContent.contains("localprofile:")) {
                throw new RuntimeException("Generated service does not use generated IG module imports");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed validating generated service content", e);
        }
    }

    private static void cleanOutputDirectory(Path outputPath) {
        File file = outputPath.toFile();
        if (!file.exists()) {
            return;
        }
        delete(file);
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    delete(child);
                }
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to delete test artifact: " + file.getAbsolutePath());
        }
    }
}
