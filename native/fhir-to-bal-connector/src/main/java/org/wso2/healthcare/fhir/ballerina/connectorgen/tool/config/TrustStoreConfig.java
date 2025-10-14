package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.config;

import com.google.gson.JsonObject;

public class TrustStoreConfig {

    private String trustStore;
    private String trustStorePassword;

    public TrustStoreConfig (JsonObject trustStoreConfigJson) {
        this.trustStore = trustStoreConfigJson.getAsJsonPrimitive("trustStoreName").getAsString();
        this.trustStorePassword = trustStoreConfigJson.getAsJsonPrimitive("trustStorePassword").getAsString();
    }

    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
}
