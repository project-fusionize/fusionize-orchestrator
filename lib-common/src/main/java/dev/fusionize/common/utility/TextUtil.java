package dev.fusionize.common.utility;

import org.apache.commons.text.CaseUtils;
import org.apache.commons.text.WordUtils;

public class TextUtil {
    public static String camelCase(String s){
        String normalized = normalize(s);
        return CaseUtils.toCamelCase(normalized, false);
    }

    public static String kebabCase(String s){
        String normalized = normalize(s);
        normalized = normalized.toLowerCase();
        normalized = normalized.replaceAll(" ","-");
        return normalized;
    }


    public static String capitalize(String s){
        return WordUtils.capitalize(normalize(s));
    }

    public static String initials(String s){
        return WordUtils.initials(normalize(s));
    }

    private static String normalize(String s){
        if(s==null) return null;
        String normalized = s.trim();
        normalized = normalized.replaceAll(" +"," ");
        return normalized;
    }
}
