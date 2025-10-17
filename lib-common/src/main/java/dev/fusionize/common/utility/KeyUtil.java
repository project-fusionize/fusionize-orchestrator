package dev.fusionize.common.utility;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.StringWriter;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

public class KeyUtil {

    public static String getNumericKey(long seed) {
        int ascii= Math.toIntExact(48 + (seed % 10));
        return Character.toString ((char) ascii);
    }
    public static String getRandomNumericKey(int length) {
        return getKey(length, true, false);
    }

    public static String getAlphabeticalKey(long seed) {
        int ascii= Math.toIntExact(65 + (seed % 26));
        return Character.toString ((char) ascii);
    }

    public static String getRandomAlphabeticalKey(int length) {
        return getKey(length, false, true);
    }

    public static String getAlphaNumericKey(long seed) {
        if(seed % 36 < 10)
            return getNumericKey(seed);
        else return getAlphabeticalKey(seed);
    }

    public static String getRandomAlphaNumericKey(int length) {
        return getKey(length, true, true);
    }

    private static String getKey(int length, boolean numeric, boolean alphabetic) {
        Random random = new Random();
        StringWriter keyStringWriter=new StringWriter();
        if(length<1) length=5;
        for(int i=0;i<length;i++){
            int seed = random.nextInt(26);
            keyStringWriter.append(
                    numeric && alphabetic ? getAlphaNumericKey(seed) :
                            alphabetic ? getAlphabeticalKey(seed) : getNumericKey(seed)
            );
        }
        return keyStringWriter.toString();
    }

    public static String getHash(String sequence) {
        return DigestUtils.sha256Hex(sequence);
    }
    public static String getRandomHash() {
        Date d = new Date();
        return DigestUtils.sha256Hex(d.getTime() + getRandomAlphaNumericKey(8));
    }

    public static String getLongHash(String sequence, int length) {
        StringWriter hashStringWriter=new StringWriter();
        if(length<1) length=5;
        for(int i=0;i<length;i++){
            String newSequence=sequence+getHash(String.valueOf(i));
            hashStringWriter.append(getHash(newSequence));
        }
        return hashStringWriter.toString();
    }

    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public static String getFlatUUID() {
        String uuid = getUUID();
        return uuid.replaceAll("-","");
    }
}
