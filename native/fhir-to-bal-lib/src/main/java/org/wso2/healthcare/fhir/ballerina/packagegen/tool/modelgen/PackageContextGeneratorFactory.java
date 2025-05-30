package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen;

import org.wso2.healthcare.codegen.tool.framework.commons.core.SpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.versions.r4.R4PackageContextGenerator;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.versions.r5.R5PackageContextGenerator;

import java.util.Map;

public class PackageContextGeneratorFactory {
    public static AbstractPackageContextGenerator getPackageContextGenerator(String fhirVersion, BallerinaPackageGenToolConfig config,
                                                                             Map<String, FHIRImplementationGuide > igEntries, SpecificationData specificationData) {

        switch (fhirVersion.toLowerCase()) {
            case ("r4"):
                return new R4PackageContextGenerator(config, igEntries, specificationData);
            case ("r5"):
                return new R5PackageContextGenerator(config, igEntries, specificationData);
            default:
                throw new IllegalArgumentException("Unsupported FHIR version: " + fhirVersion);
        }
    }
}
