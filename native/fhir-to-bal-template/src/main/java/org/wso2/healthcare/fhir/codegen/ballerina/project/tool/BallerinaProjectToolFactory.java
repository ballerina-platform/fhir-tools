package org.wso2.healthcare.fhir.codegen.ballerina.project.tool;

import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.versions.r4.R4BallerinaProjectTool;
import org.wso2.healthcare.fhir.codegen.ballerina.project.tool.versions.r5.R5BallerinaProjectTool;

public class BallerinaProjectToolFactory {
    public AbstractBallerinaProjectTool getBallerinaProjectTool(String fhirVersion) {
        if (fhirVersion.equals("r4")) {
            return new R4BallerinaProjectTool();
        }
        else if (fhirVersion.equals("r5")) {
            return new R5BallerinaProjectTool();
        }
        else {
            throw new IllegalArgumentException("Unsupported FHIR version: " + fhirVersion);
        }
    }
}
