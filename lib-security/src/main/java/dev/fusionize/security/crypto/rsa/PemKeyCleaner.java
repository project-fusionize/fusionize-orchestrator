package dev.fusionize.security.crypto.rsa;

import java.util.Base64;

public class PemKeyCleaner {
    // Common PEM headers for private and public keys
    private static final String[] PRIVATE_KEY_HEADERS = {
            "-----BEGIN PRIVATE KEY-----",
            "-----END PRIVATE KEY-----",
            "-----BEGIN RSA PRIVATE KEY-----",
            "-----END RSA PRIVATE KEY-----"
    };

    private static final String[] PUBLIC_KEY_HEADERS = {
            "-----BEGIN PUBLIC KEY-----",
            "-----END PUBLIC KEY-----",
            "-----BEGIN RSA PUBLIC KEY-----",
            "-----END RSA PUBLIC KEY-----"
    };

    /**
     * Removes PEM headers and footers from any RSA private or public key.
     * @param pemKey The PEM formatted key content.
     * @return A clean Base64 string with only the key body.
     */
    public static String cleanPem(String pemKey) {
        if (pemKey == null) {
            throw new IllegalArgumentException("PEM key content cannot be null");
        }

        String cleaned = pemKey
                .replaceAll("\\r", "")
                .replaceAll("\\n", "")
                .trim();

        for (String header : PRIVATE_KEY_HEADERS) {
            cleaned = cleaned.replace(header, "");
        }
        for (String header : PUBLIC_KEY_HEADERS) {
            cleaned = cleaned.replace(header, "");
        }

        cleaned = cleaned.replaceAll("\\s+", "");

        if (!isBase64(cleaned)) {
            throw new IllegalArgumentException("Key content is not valid Base64 after cleanup.");
        }

        return cleaned;
    }

    /**
     * Quick Base64 validation helper
     */
    private static boolean isBase64(String value) {
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
