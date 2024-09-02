/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.cds.codegen.ballerina.tool.generator;

import org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants;
import org.wso2.healthcare.cds.codegen.ballerina.tool.config.BallerinaProjectToolConfig;
import org.wso2.healthcare.cds.codegen.ballerina.tool.model.BallerinaService;
import org.wso2.healthcare.cds.codegen.ballerina.tool.model.CdsHook;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.AbstractFHIRTemplateGenerator;

import java.io.Console;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.CMD_MESSAGE_OVERRIDE_OUTPUT_DIRECTORY;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.CONFIG;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.NO;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.ONE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.REGEX_STRING_FOR_NON_WORD_CHARACTER;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.REGEX_STRING_FOR_UNDERSCORE_AND_HYPHEN;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.SERVICE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.UNDERSCORE;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.YES;
import static org.wso2.healthcare.cds.codegen.ballerina.tool.CdsBallerinaProjectConstants.ZERO;


/**
 * Generator class to wrap all the generator classes in Ballerina project generator.
 */
public class BallerinaProjectGenerator extends AbstractFHIRTemplateGenerator {

    public BallerinaProjectGenerator(String targetDir) throws CodeGenException {
        super(targetDir);
    }

    @Override
    public void generate(ToolContext toolContext, Map<String, Object> generatorProperties) throws CodeGenException {

        BallerinaProjectToolConfig ballerinaProjectToolConfig = (BallerinaProjectToolConfig) generatorProperties.get(CONFIG);
        String packagePath = this.getTargetDir() + ballerinaProjectToolConfig.getMetadataConfig().getNamePrefix() + File.separator;

        // Provide option to check and overwrite the existing package
        Console console = System.console();
        if (console != null && Files.exists(Paths.get(packagePath))) {
            String input = console.readLine(CMD_MESSAGE_OVERRIDE_OUTPUT_DIRECTORY);
            if (NO.equalsIgnoreCase(input)) {
                System.exit(ZERO);
            } else if (YES.equalsIgnoreCase(input)) {
                System.out.println(CdsBallerinaProjectConstants.PrintStrings.OVERWRITING_EXISTING_TEMPLATES);
            } else {
                System.out.println(CdsBallerinaProjectConstants.PrintStrings.INVALID_INPUT);
                System.exit(ZERO);
            }
        }

        BallerinaService ballerinaService = populateServiceObject(generatorProperties);
        generatorProperties.put(SERVICE, ballerinaService);

        ServiceGenerator balServiceGenerator = new ServiceGenerator(packagePath);
        balServiceGenerator.generate(toolContext, generatorProperties);

        TomlGenerator tomlGenerator = new TomlGenerator(packagePath);
        tomlGenerator.generate(toolContext, generatorProperties);

        MetaGenerator metaGenerator = new MetaGenerator(packagePath);
        metaGenerator.generate(toolContext, generatorProperties);

    }

    private BallerinaService populateServiceObject(Map<String, Object> generatorProperties) {
        BallerinaService ballerinaService = new BallerinaService();
        BallerinaProjectToolConfig config = (BallerinaProjectToolConfig) generatorProperties.get(CONFIG);
        ballerinaService.setName(config.getMetadataConfig().getNamePrefix());

        Map<String, CdsHook> cdsHookMap = new HashMap<>();
        for (CdsHook cdsHook : config.getCdsHooks()) {
            String id = hookIdToCamelCase(cdsHook.getId());
            cdsHookMap.put(id, cdsHook);
        }
        ballerinaService.setCdsHooks(cdsHookMap);
        return ballerinaService;
    }

    // This method will remove special characters from the hook id and make it as camel case
    private String hookIdToCamelCase(String text) {
        text = text.replaceAll(REGEX_STRING_FOR_NON_WORD_CHARACTER, UNDERSCORE);

        for (int i = ZERO; i < text.length(); i++) {
            String subString = text.substring(i);

            Pattern p = Pattern.compile(REGEX_STRING_FOR_UNDERSCORE_AND_HYPHEN);
            Matcher matcher = p.matcher(subString);
            if (matcher.find()) {
                int matchedIndex = matcher.start();
                char nextCharAfterMatch = text.charAt(matchedIndex + i + ONE);

                StringBuilder textCopy = new StringBuilder(text);
                char nextCharUpperCase = Character.toUpperCase(nextCharAfterMatch);
                textCopy.setCharAt(matchedIndex + i + ONE, nextCharUpperCase);
                text = textCopy.toString();
                i = matchedIndex + i;
            } else {
                break;
            }
        }

        char firstCharOfText = text.charAt(ZERO);
        StringBuilder textCopy = new StringBuilder(text);
        textCopy.setCharAt(ZERO, Character.toUpperCase(firstCharOfText));
        text = textCopy.toString();
        return text;
    }
}
