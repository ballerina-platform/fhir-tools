/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.model.CapabilityStatement;

import java.io.IOException;
import java.util.List;

public class HttpUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static CapabilityStatement getCapabilityStatement(String fhirServerURL) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(fhirServerURL);
            request.setHeader("Accept", "application/fhir+json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    System.err.println("Failed: Get Capability Statement: " + status);
                    return null;
                }
                String json = EntityUtils.toString(response.getEntity());
                return mapper.readValue(json, CapabilityStatement.class);
            }
        } catch (IOException e) {
            System.err.println("Error fetching CapabilityStatement: " + e.getMessage());
            System.exit(0);
        }
        return null;
    }

    public static String getReadMe(String balCentralURL, String orgName, String packageName, String packageVersion) {
        String query = String.format(
                "{ \"query\": \"{ package ( orgName: \\\"%s\\\", packageName: \\\"%s\\\", version : \\\"%s\\\") { readme } }\" }",
                orgName, packageName, packageVersion
        );
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String requestURL = balCentralURL + "/graphql";
            HttpPost request = new HttpPost(requestURL);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(query));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseString = EntityUtils.toString(response.getEntity());
//                System.out.println("Response: " + responseString);

                if (responseString == null || responseString.isEmpty()) {
                    System.err.println("Empty response from Ballerina Central. " +
                            "Please check the Ballerina Central URL and organization name");
                    System.exit(0);
                }

                JsonObject responseObject = JsonParser.parseString(responseString).getAsJsonObject();
                JsonObject dataObj = responseObject.has("data") ? responseObject.getAsJsonObject("data") : null;
                JsonObject packageObj = (dataObj != null && dataObj.has("package")) ? dataObj.getAsJsonObject("package") : null;

                if (packageObj != null && packageObj.has("readme")) {
                    return packageObj.get("readme").getAsString();
                } else {
                    System.err.println("README not found for package: " + packageName + " version: " + packageVersion);
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching README from Ballerina Central: " + e.getMessage());
            System.exit(0);
        }
        return null;
    }

    public static String getLatestVersionOfPackage(String ballerinaCentralURL, String orgName, String packageName) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String requestURL = String.format("%s/%s/%s/%s", ballerinaCentralURL,"/registry/packages", orgName, packageName);
            HttpGet request = new HttpGet(requestURL);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    System.err.println("Failed: Get Package Version: " + status);
                    return null;
                }
                String json = EntityUtils.toString(response.getEntity());
                List<String> versions = mapper.readValue(json, new TypeReference<>() {});
                if (versions != null && !versions.isEmpty()) {
                    return CommonUtils.getLatestVersion(versions);
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching package versions for " + packageName + ": " + e.getMessage());
            System.exit(0);
        }
        return null;
    }
}
