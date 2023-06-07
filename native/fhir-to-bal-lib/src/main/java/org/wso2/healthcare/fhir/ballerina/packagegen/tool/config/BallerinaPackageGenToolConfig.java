// Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

package org.wso2.healthcare.fhir.ballerina.packagegen.tool.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.consensys.cava.toml.TomlArray;
import net.consensys.cava.toml.TomlParseResult;
import net.consensys.cava.toml.TomlTable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tooling.common.Constants;
import org.wso2.healthcare.codegen.tooling.common.config.AbstractToolConfig;
import org.wso2.healthcare.codegen.tooling.common.exception.CodeGenException;
import org.wso2.healthcare.codegen.tooling.common.model.ConfigType;
import org.wso2.healthcare.codegen.tooling.common.model.JsonConfigType;
import org.wso2.healthcare.codegen.tooling.common.model.TomlConfigType;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ballerina Package Generator Tool related config wrapper.
 */
public class BallerinaPackageGenToolConfig extends AbstractToolConfig {

    private static final Log LOG = LogFactory.getLog(BallerinaPackageGenToolConfig.class);
    private final Map<String, IncludedIGConfig> includedIGConfigs = new HashMap<>();
    private final Map<String, DataTypeMappingConfig> dataTypeMappingConfigs = new HashMap<>();
    private final Map<String, BallerinaKeywordConfig> ballerinaKeywordConfig = new HashMap<>();
    private PackageConfig packageConfig;
    private boolean isEnabled;

    /**
     * Populate specific tool configs from tool-config.json
     *
     * @param configObj raw config object
     * @throws CodeGenException CodeGen Exception
     */
    @Override
    public void configure(ConfigType<?> configObj) throws CodeGenException {
        LOG.debug("Started: Ballerina Package Generator Tool config population");
        if (Constants.JSON_CONFIG_TYPE.equals(configObj.getType())) {
            JsonObject jsonConfigObj = ((JsonConfigType) configObj).getConfigObj();
            this.isEnabled = jsonConfigObj.getAsJsonPrimitive(ToolConstants.CONFIG_ENABLE).getAsBoolean();
            this.packageConfig = new PackageConfig(jsonConfigObj.get(ToolConstants.CONFIG_PACKAGE).getAsJsonObject());

            populateIgConfigs(jsonConfigObj.getAsJsonArray(ToolConstants.CONFIG_INCLUDED_IGS));
            populateDataTypeConfigs(jsonConfigObj.getAsJsonArray(ToolConstants.CONFIG_DATA_TYPE_MAPPINGS));
            populateBallerinaKeywordConfigs(jsonConfigObj.getAsJsonArray(ToolConstants.CONFIG_BALLERINA_KEYWORD));
        } else if (Constants.TOML_CONFIG_TYPE.equals(configObj.getType())) {
            TomlParseResult tomlConfigObj = ((TomlConfigType) configObj).getConfigObj();

            this.isEnabled = Boolean.TRUE.equals(tomlConfigObj.getBoolean(ToolConstants.CONFIG_ENABLE));

            Object packageConfigObj = tomlConfigObj.get(ToolConstants.CONFIG_PACKAGE_TOML);
            if (packageConfigObj instanceof TomlTable) {
                TomlTable packageConfig = (TomlTable) packageConfigObj;
                this.packageConfig = new PackageConfig(packageConfig);
            }

            Object includedIGObj = tomlConfigObj.get(ToolConstants.CONFIG_INCLUDED_IGS_TOML);
            if (includedIGObj instanceof TomlArray) {
                populateIgConfigs((TomlArray) includedIGObj);
            }

            Object dataTypesMappingObj = tomlConfigObj.get(ToolConstants.CONFIG_DATA_TYPE_MAPPING_TOML);
            if (dataTypesMappingObj instanceof TomlArray) {
                populateDataTypeConfigs((TomlArray) dataTypesMappingObj);
            }

            Object ballerinaKeywordsObj = tomlConfigObj.get(ToolConstants.CONFIG_BALLERINA_KEYWORD_TOML);
            if (ballerinaKeywordsObj instanceof TomlArray) {
                populateBallerinaKeywordConfigs((TomlArray) ballerinaKeywordsObj);
            }
        }
        LOG.debug("Ended: Ballerina Package Generator Tool config population");
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public PackageConfig getPackageConfig() {
        return packageConfig;
    }

    private void populateIgConfigs(JsonArray igArray) {
        LOG.debug("Started: IG config population");
        for (int i = 0; i < igArray.size(); i++) {
            includedIGConfigs.put(
                    igArray.get(i).getAsJsonObject().getAsJsonPrimitive(ToolConstants.CONFIG_PROFILE_IG).getAsString(),
                    new IncludedIGConfig(igArray.get(i).getAsJsonObject()));
        }
        LOG.debug("Ended: IG config population");
    }

    private void populateIgConfigs(TomlArray igArray) {
        LOG.debug("Started: IG config population");
        List<Object> tomlList = igArray.toList();
        for (Object toolsProjectIncludedIG : tomlList) {
            if (toolsProjectIncludedIG instanceof TomlTable) {
                includedIGConfigs.put(((TomlTable) toolsProjectIncludedIG).getString(
                        ToolConstants.CONFIG_PROFILE_IG_TOML), new IncludedIGConfig(
                        (TomlTable) toolsProjectIncludedIG));
            }
        }
        LOG.debug("Ended: IG config population");
    }

    private void populateDataTypeConfigs(JsonArray jsonArray) {
        LOG.debug("Started: data type config population");
        for (int i = 0; i < jsonArray.size(); i++) {
            dataTypeMappingConfigs.put(
                    jsonArray.get(i).getAsJsonObject().getAsJsonPrimitive(ToolConstants.CONFIG_DATA_TYPE_FHIR).getAsString(),
                    new DataTypeMappingConfig(jsonArray.get(i).getAsJsonObject()));
        }
        LOG.debug("Ended: data type config population");
    }

    private void populateDataTypeConfigs(TomlArray tomlArray) {
        LOG.debug("Started: data type config population");
        List<Object> tomlList = tomlArray.toList();
        for (Object dataTypesMap : tomlList) {
            if (dataTypesMap instanceof TomlTable) {
                includedIGConfigs.put(((TomlTable) dataTypesMap).getString(
                                ToolConstants.CONFIG_DATA_TYPE_FHIR_TOML),
                        new IncludedIGConfig((TomlTable) dataTypesMap));
            }
        }
        LOG.debug("Ended: data type config population");
    }

    private void populateBallerinaKeywordConfigs(JsonArray jsonArray) {
        LOG.debug("Started: Ballerina keywords config population");
        for (int i = 0; i < jsonArray.size(); i++) {
            ballerinaKeywordConfig.put(jsonArray.get(i).getAsJsonObject()
                    .getAsJsonPrimitive(ToolConstants.CONFIG_BALLERINA_KEYWORD_KEYWORD)
                    .getAsString(), new BallerinaKeywordConfig(jsonArray.get(i).getAsJsonObject()));
        }
        LOG.debug("Ended: Ballerina keywords config population");
    }

    private void populateBallerinaKeywordConfigs(TomlArray tomlArray) {
        LOG.debug("Started: Ballerina keywords config population");
        List<Object> tomlList = tomlArray.toList();
        for (Object keywordMap : tomlList) {
            if (keywordMap instanceof TomlTable) {
                ballerinaKeywordConfig.put(((TomlTable) keywordMap)
                                .getString(ToolConstants.CONFIG_BALLERINA_KEYWORD_KEYWORD_TOML),
                        new BallerinaKeywordConfig((TomlTable) keywordMap));
            }
        }
        LOG.debug("Ended: Ballerina keywords config population");
    }

    public Map<String, IncludedIGConfig> getIncludedIGConfigs() {
        return includedIGConfigs;
    }

    public Map<String, DataTypeMappingConfig> getDataTypeMappingConfigs() {
        return dataTypeMappingConfigs;
    }

    public Map<String, BallerinaKeywordConfig> getBallerinaKeywordConfig() {
        return ballerinaKeywordConfig;
    }
}