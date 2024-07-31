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

import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.health.cmd.core.exception.BallerinaHealthException;
import io.ballerina.health.cmd.core.utils.HealthCmdUtils;
import io.ballerina.health.cmd.handler.Handler;
import io.ballerina.health.cmd.handler.HandlerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TestRunner {
    private static final Path executionPath = Paths.get(System.getProperty("user.dir"));

    public static void main(String[] args) {
        Map<String, Object> argsMap = new HashMap<>();
        String packageName = "health.fhir.r4.uscore501";
        String orgName = "ballerinax";
        String packageVersion = "1.1.0";
        argsMap.put("--package-name", packageName);
        argsMap.put("--org-name", orgName);
        argsMap.put("--package-version", packageVersion);
        argsMap.put("--included-profile", null);
        argsMap.put("--excluded-profile", null);
        argsMap.put("--dependency", null);
        String mode = "package";
        String command = "fhir";

        String resourcePath = Paths.get(Objects.requireNonNull(TestRunner.class.getClassLoader().getResource("io")).getPath()).getParent().getParent().toString() + "/test-classes" + "/profiles.USCore";
        File resourcesDirectory = new File(resourcePath);
        String specPath = resourcesDirectory.getAbsolutePath();
        String outPutPath = Paths.get(Objects.requireNonNull(TestRunner.class.getClassLoader().getResource("io")).getPath()).getParent().getParent().toString() + "/test-classes";

        //spec path is the last argument
        Path specificationPath;
        try {
            specificationPath = HealthCmdUtils.validateAndSetSpecificationPath(specPath, executionPath.toString());
        } catch (BallerinaHealthException e) {
            System.out.println("Invalid specification path received for FHIR tool command.");
            throw new BLauncherException();
        }
        Handler toolHandler = null;
        try {
            toolHandler = HandlerFactory.createHandler(command, mode, System.out, specificationPath.toString());
        } catch (BallerinaHealthException e) {
            System.out.println(e);
            throw new BLauncherException();
        }

        toolHandler.setArgs(argsMap);
        toolHandler.execute(specificationPath.toString(), getTargetOutputPath(outPutPath).toString());
    }

    private static Path getTargetOutputPath(String outputPath){
        Path targetOutputPath = TestRunner.executionPath;
        if (outputPath != null) {
            if (Paths.get(outputPath).isAbsolute()) {
                targetOutputPath = Paths.get(outputPath);
            } else {
                targetOutputPath = Paths.get(targetOutputPath.toString(), outputPath);
            }
        } else {
            targetOutputPath = Paths.get(targetOutputPath + File.separator + "generated");
        }
        return targetOutputPath;
    }
}
