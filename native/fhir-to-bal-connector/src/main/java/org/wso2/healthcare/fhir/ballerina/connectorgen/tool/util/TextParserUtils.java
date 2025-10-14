package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.util;

import org.wso2.healthcare.fhir.ballerina.connectorgen.tool.model.FHIRResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParserUtils {

    public static final String FOOTNOTE_REGEX = "^\\[(m\\d+|s\\d+)]:\\s*(\\S+)";
    public static final String TABLE_ROW_REGEX = "(?m)^\\|\\s*\\d+\\).*\\|.*\\|";
    public static final String HEADER_REGEX = "\\|[-\\s|]+\\|";
    public static final String SEPERATOR_REGEX = "\\|";
    public static final String NUMBER_BULLET_REGEX = "\\d+\\)\\.\\s*";
    public static final String URL_EXTRACTION_REGEX = "\\[(m\\d+|s\\d+)]";
    public static final String SPACE_REGEX = "\\s+";
    public static final String EMPTY_SPACE = "";

    /**
     * Parses the README.md content to extract FHIR resource information.
     *
     * @param readMe The content of the README.md file as a string.
     * @return A map where the key is the resource definition URL and the value is a FHIRResource object.
     */
    public static Map<String, FHIRResource> parseReadMeString (String readMe, String packageName, String packageAlias) {

        Map<String, FHIRResource> resources = new HashMap<>();

        // Parse footnotes ([m1]: URL and [s1]: URL)
        Map<String, String> footnotes = new HashMap<>();
        Pattern footnotePattern = Pattern.compile(FOOTNOTE_REGEX, Pattern.MULTILINE);
        Matcher footnoteMatcher = footnotePattern.matcher(readMe);
        while (footnoteMatcher.find()) {
            footnotes.put(footnoteMatcher.group(1), footnoteMatcher.group(2));
        }

        // Parse FHIR resources table rows
        Pattern tablePattern = Pattern.compile(TABLE_ROW_REGEX);
        Matcher matcher = tablePattern.matcher(readMe);

        while (matcher.find()) {
            String line = matcher.group().trim();
            // Skip header or separator lines
            if (line.isEmpty() || line.matches(HEADER_REGEX)) continue;

            String[] cols = line.split(SEPERATOR_REGEX);
            if (cols.length < 3) continue;

            // Clean up the resource name: remove numbering "1). "
            String name = cols[1].trim().replaceAll(NUMBER_BULLET_REGEX, EMPTY_SPACE);

            String urlCol = cols[2].trim();
            String urlString = extractUrl(urlCol, footnotes);
            String[] urls = urlString.split(SPACE_REGEX);
            String definitionUrl = urls.length > 0 ? urls[0] : EMPTY_SPACE;
            String recordUrl = urls.length > 1 ? urls[1] : EMPTY_SPACE;


            resources.put(definitionUrl, new FHIRResource(name, definitionUrl, recordUrl, packageName, packageAlias));
        }

        return resources;
    }

    /**
     * Extracts URL references from a text column and replace it with the URLs from footnotes.
     *
     * @param text      The text column containing footnote references.
     * @param footnotes A map of footnote keys to their corresponding URLs.
     * @return A space-separated string of URLs.
     */
    private static String extractUrl(String text, Map<String, String> footnotes) {
        Matcher refMatcher = Pattern.compile(URL_EXTRACTION_REGEX).matcher(text);
        List<String> urls = new ArrayList<>();
        while (refMatcher.find()) {
            String key = refMatcher.group(1);
            if (footnotes.containsKey(key)) urls.add(footnotes.get(key));
        }
        return String.join(" ", urls);
    }


    /**
     * Extracts the comment related to a specific keyword from a multi-resource section in the input text.
     *
     * @param input   The input string containing multiple resources section.
     * @param keyword The keyword to search for in the resource list.
     * @return The extracted resource text if found, otherwise the original input with new lines removed.
     */
    public static String extractCommentFromText(String input, String keyword) {
        if (!input.contains("Multiple Resources:")) {
            return StringUtils.removeNewLines(input); // No section found
        }

        // Regex for line like: * [Name](url): description
        Pattern pattern = Pattern.compile("\\* \\[(.+?)]\\((.+?)\\): (.+)");
        for (String line : input.split("\\r?\\n")) {
            line = line.trim();
            if (line.startsWith("* [") && line.contains("[" + keyword + "]")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group();
                }
            }
        }
        return StringUtils.removeNewLines(input); // Not found
    }
}
