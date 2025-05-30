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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TestRunner {
    private static final Path executionPath = Paths.get(System.getProperty("user.dir"));

    public static void main(String[] args) {
        // System.setProperty("fhirVersion", "r4"); // Uncomment if directly running the TestRunner.java
        try{
            runTestForCdsTool();
            if(System.getProperty("fhirVersion").equalsIgnoreCase("r4")){
                runTestForR4FhirTool();
            }
            else{
                runTestForR5FhirTool();
            }
        }
        catch (URISyntaxException e){
            e.printStackTrace();
        }
    }

    private static void runTestForCdsTool() throws URISyntaxException {

        Map<String, Object> argsMap = new HashMap<>();
        String packageName = "health.fhir.cds";
        String orgName = "ballerinax";
        String packageVersion = "1.1.0";
        String fhirVersion = "r4";
        argsMap.put("--package-name", packageName);
        argsMap.put("--org-name", orgName);
        argsMap.put("--package-version", packageVersion);
        argsMap.put("--fhir-version", fhirVersion);
        String mode = "template";
        String command = "cds";

        String resourcePath = Paths.get(Objects.requireNonNull(TestRunner.class.getClassLoader().getResource("io")).toURI()).getParent().getParent().toString() + File.separator + "test-classes" + File.separator + "cds.hooks";
        System.out.println("Resource Path: " + resourcePath);

        File resourcesDirectory = new File(resourcePath);
        String specPath = resourcesDirectory.getAbsolutePath();
        System.out.println("Spec Path: " + specPath);

        String outPutPath = Paths.get(Objects.requireNonNull(TestRunner.class.getClassLoader().getResource("io")).toURI()).getParent().getParent().toString() + File.separator + "test-classes";
        System.out.println("Output Path: " + outPutPath);

        //spec path is the last argument
        Path specificationPath;

        try {
            specificationPath = HealthCmdUtils.getSpecificationPath(specPath, executionPath.toString());
        } catch (BallerinaHealthException e) {
            System.out.println("Invalid specification path received.");
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
        toolHandler.execute(specificationPath+File.separator+"tool-config.toml", getTargetOutputPath(outPutPath).toString());
    }

    private static void runTestForR4FhirTool() throws URISyntaxException {
        Map<String, Object> argsMap = new HashMap<>();
        String packageName = "health.fhir.r4.uscore501";
        String orgName = "ballerinax";
        String packageVersion = "1.1.0";
        String fhirVersion = "r4";
        argsMap.put("--package-name", packageName); // FOR PACKAGE
        argsMap.put("--package-version", packageVersion); // FOR PACKAGE
        argsMap.put("--dependency", null); // FOR PACKAGE
        argsMap.put("--org-name", orgName);
//        argsMap.put("--dependent-package", orgName + "/" + packageName); // FOR TEMPLATE
        argsMap.put("--fhir-version", fhirVersion);
        argsMap.put("--included-profile", null);
        argsMap.put("--excluded-profile", null);
        String mode = "package";
        String command = "fhir";

        String resourcePath = Paths.get(Objects.requireNonNull(TestRunner.class.getClassLoader().getResource("io")).toURI()).getParent().getParent().toString() + File.separator + "test-classes" + File.separator + "profiles.USCore";
        System.out.println("Resource Path: " + resourcePath);

        File resourcesDirectory = new File(resourcePath);
        String specPath = resourcesDirectory.getAbsolutePath();
        System.out.println("Spec Path: " + specPath);

        String outPutPath = Paths.get(Objects.requireNonNull(TestRunner.class.getClassLoader().getResource("io")).toURI()).getParent().getParent().toString() + File.separator + "test-classes";
        System.out.println("Output Path: " + outPutPath);

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

    private static void runTestForR5FhirTool() throws URISyntaxException {
        Map<String, Object> argsMap = new HashMap<>();
        String packageName = "health.fhir.r5.europebase";
        String orgName = "ballerinax";
        String packageVersion = "1.1.0";
        String fhirVersion = "r5";
        argsMap.put("--package-name", packageName); // FOR PACKAGE
        argsMap.put("--package-version", packageVersion); // FOR PACKAGE
        argsMap.put("--dependency", null); // FOR PACKAGE
        argsMap.put("--org-name", orgName);
//        argsMap.put("--dependent-package", orgName + "/" + packageName); // FOR TEMPLATE
        argsMap.put("--fhir-version", fhirVersion);
        argsMap.put("--included-profile", null);
        argsMap.put("--excluded-profile", null);
        String mode = "package";
        String command = "fhir";

        String resourcePath = Paths.get(Objects.requireNonNull(TestRunner.class.getClassLoader().getResource("io")).toURI()).getParent().getParent().toString() + File.separator + "test-classes" + File.separator + "profiles.EuropeBase";
        System.out.println("Resource Path: " + resourcePath);

        File resourcesDirectory = new File(resourcePath);
        String specPath = resourcesDirectory.getAbsolutePath();
        System.out.println("Spec Path: " + specPath);

        String outPutPath = Paths.get(Objects.requireNonNull(TestRunner.class.getClassLoader().getResource("io")).toURI()).getParent().getParent().toString() + File.separator + "test-classes";
        System.out.println("Output Path: " + outPutPath);

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