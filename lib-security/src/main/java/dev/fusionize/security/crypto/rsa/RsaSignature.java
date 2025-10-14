package dev.fusionize.security.crypto.rsa;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;

public class RsaSignature {
    public static String sign(String message, String base64PrivateKey)
            throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(RsaDecrypter.getPrivateKey(base64PrivateKey));
        privateSignature.update(message.getBytes(StandardCharsets.UTF_8));
        byte[] signature = privateSignature.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    public static boolean verify(String message, String base64Signature, String base64PublicKey)
            throws Exception {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(RsaEncrypter.getPublicKey(base64PublicKey));
        publicSignature.update(message.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Base64.getDecoder().decode(base64Signature);
        return publicSignature.verify(signatureBytes);
    }
}
