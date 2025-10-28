package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.commons.config.AbstractToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.commons.model.ConfigType;
import org.wso2.healthcare.codegen.tool.framework.commons.model.JsonConfigType;

public class BallerinaConnectorGenToolConfig extends AbstractToolConfig {

    private static final Log LOG = LogFactory.getLog(BallerinaConnectorGenToolConfig.class);
//    private TrustStoreConfig trustStoreConfig;
    private CentralConfig centralConfig;
    private String fhirServerUrl;

    @Override
    public void configure(ConfigType<?> configObj) {
        LOG.debug("Started: Ballerina Connector Generator Tool config population");
        JsonObject jsonConfigObj = ((JsonConfigType) configObj).getConfigObj();
        this.centralConfig = new CentralConfig(jsonConfigObj.getAsJsonObject("central"));
        this.fhirServerUrl = jsonConfigObj.get("fhirServerUrl").getAsString();
        LOG.debug("Ended: Ballerina Connector Generator Tool config population");
    }

    @Override
    public void overrideConfig(String s, JsonElement jsonElement) {
        //TODO: implement if required
    }



    public CentralConfig getCentralConfig() {
        return centralConfig;
    }

    public void setCentralConfig(CentralConfig centralConfig) {
        this.centralConfig = centralConfig;
    }

    public String getFhirServerUrl() {
        return fhirServerUrl;
    }

    public void setFhirServerUrl(String fhirServerUrl) {
        this.fhirServerUrl = fhirServerUrl;
    }
}
