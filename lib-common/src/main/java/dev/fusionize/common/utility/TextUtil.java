package dev.fusionize.common.utility;

import org.apache.commons.text.CaseUtils;
import org.apache.commons.text.WordUtils;

import java.util.Arrays;

public class TextUtil {

    /** -------------------- Public Converters -------------------- **/

    public static String camelCase(String s){
        String n = normalize(s);
        if (n.matches("[a-z][A-Za-z0-9]*") || n.matches("[A-Z][A-Za-z0-9]*")) {
            return n.substring(0,1).toLowerCase() + n.substring(1);
        }
        return CaseUtils.toCamelCase(n, false);
    }

    public static String kebabCase(String s){
        String normalized = normalize(s);
        normalized = normalized.toLowerCase().replaceAll(" ", "-");
        return normalized;
    }

    public static String snakeCase(String s){
        String n = normalize(s);
        return n.toLowerCase().replaceAll(" ", "_");
    }

    public static String capitalize(String s){
        return WordUtils.capitalize(normalize(s));
    }

    public static String initials(String s){
        return WordUtils.initials(normalize(s));
    }

    /** -------------------- Reverse Converters -------------------- **/

    // Convert camelCase → "camel case"
    public static String fromCamelCase(String s){
        if (s == null) return null;
        String spaced = s.replaceAll("(?<!^)([A-Z])", " $1");
        return normalize(spaced.toLowerCase());
    }

    // Convert kebab-case → "kebab case"
    public static String fromKebabCase(String s){
        if (s == null) return null;
        String spaced = s.replaceAll("-", " ");
        return normalize(spaced.toLowerCase());
    }

    // Convert kebab_case → "kebab case"
    public static String fromSnakeCase(String s){
        if (s == null) return null;
        return normalize(s.replaceAll("_", " ").toLowerCase());
    }

    /** -------------------- Flexible Matching -------------------- **/

    /**
     * Matches two keys ignoring format differences such as:
     *  - camelCase
     *  - kebab-case
     *  - capitalization
     *  - spacing
     *  - extra spaces
     *
     * Returns true if any normalized representation matches.
     */
    public static boolean matchesFlexible(String expected, String actual){
        if (expected == null || actual == null) return false;

        String eNorm = normalize(expected);
        String aNorm = normalize(actual);

        // All formats of expected
        String[] expectedForms = new String[] {
                eNorm,
                eNorm.toLowerCase(),
                camelCase(eNorm),
                camelCase(eNorm).toLowerCase(),
                kebabCase(eNorm),
                snakeCase(eNorm),
                snakeCase(eNorm).toUpperCase(),
                fromCamelCase(eNorm),
                fromCamelCase(eNorm).toLowerCase(),
                fromKebabCase(eNorm),
                fromKebabCase(eNorm).toLowerCase(),
                fromSnakeCase(eNorm),
                fromSnakeCase(eNorm).toLowerCase(),
        };

        // All formats of actual
        String[] actualForms = new String[] {
                aNorm,
                aNorm.toLowerCase(),
                camelCase(aNorm),
                camelCase(aNorm).toLowerCase(),
                kebabCase(aNorm),
                snakeCase(aNorm),
                snakeCase(aNorm).toUpperCase(),
                fromCamelCase(aNorm),
                fromCamelCase(aNorm).toLowerCase(),
                fromKebabCase(aNorm),
                fromKebabCase(aNorm).toLowerCase(),
                fromSnakeCase(aNorm),
                fromSnakeCase(aNorm).toLowerCase(),
        };

        // Cross-compare
        for (String e : expectedForms) {
            for (String a : actualForms) {
                if (e.equals(a)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** -------------------- Utils -------------------- **/

    private static String normalize(String s){
        if(s==null) return null;
        String normalized = s.trim();
        normalized = normalized.replaceAll(" +"," ");
        return normalized;
    }
}
