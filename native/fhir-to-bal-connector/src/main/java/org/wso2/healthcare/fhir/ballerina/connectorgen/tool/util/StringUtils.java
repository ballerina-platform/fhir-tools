package org.wso2.healthcare.fhir.ballerina.connectorgen.tool.util;

import java.util.regex.Pattern;

public class StringUtils {


    // TODO: Replace from the method in BallerinaProjectUtil once integrated
    public static String resolveSpecialCharacters(String specialChar) {
        return specialChar.replaceAll(Pattern.quote("[x]"), "")
                .replaceAll(Pattern.quote("/"), "")
                .replaceAll("-", "_")
                .replaceAll("\\.", "_")
                .replaceAll("\\s+", "");
    }

    //append an underscore if the string is a Ballerina keyword
    public static String handleBallerinaKeyword(String str) {
        String[] keywords = {"abort", "aborted", "abstract", "all", "and", "any", "as", "break", "but", "check", "class", "const", "continue", "else", "elseif",
                "end", "error", "fail", "final", "for", "fork", "function", "if", "in", "isolated", "is", "join", "json", "let",
                "lock", "match", "new", "not", "object", "of", "on", "or", "panic", "private", "public", "resource", "return", "service", "some", "source", "start", "stop", "string", "table", "transaction", "trap", "true", "try", "type", "var", "while", "xml" };
        for (String keyword : keywords) {
            if (str.equals(keyword)) {
                return "'" + str;
            }
        }
        return str;
    }

    public static String removeNewLines(String text) {
        // Split by ".\n" OR just "." if newlines might vary
        String[] parts = text.split("\\.\\s*\\n?");

        if (parts.length > 0) {
            return  parts[0].trim();
        }

        return text.trim();
    }

    public static String toCamelCase(String input) {
        String[] parts = input.split("\\.");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                // first word stays lowercase
                sb.append(part.toLowerCase());
            } else {
                // capitalize first letter
                sb.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1));
            }
        }
        return sb.toString();
    }

}
