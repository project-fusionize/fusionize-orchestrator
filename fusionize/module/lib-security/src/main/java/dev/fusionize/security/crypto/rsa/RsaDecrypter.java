package dev.fusionize.security.crypto.rsa;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;

public class RsaDecrypter {

    public static PrivateKey getPrivateKey(String base64PrivateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decoded = Base64.getDecoder().decode(PemKeyCleaner.cleanPem(base64PrivateKey));
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            // Fallback for PKCS#1
            RSAPrivateKey pkcs1PrivateKey = RSAPrivateKey.getInstance(decoded);
            BigInteger modulus = pkcs1PrivateKey.getModulus();
            BigInteger privateExponent = pkcs1PrivateKey.getPrivateExponent();
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, privateExponent);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        }
    }

    public static byte[] decrypt(byte[] encryptedBytes, String base64PrivateKey)
            throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(base64PrivateKey));
        return cipher.doFinal(encryptedBytes);
    }

    public static String decryptToString(String base64Encrypted, String base64PrivateKey)
            throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(base64Encrypted);
        byte[] decrypted = decrypt(encryptedBytes, base64PrivateKey);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
