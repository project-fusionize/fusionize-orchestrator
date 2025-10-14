package dev.fusionize.security.crypto.rsa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaSignatureTest {
    private String pubKey;
    private String priKey;

    @BeforeEach
    public void init() throws IOException {
        Path workingDir = Path.of("", "src/test/resources");
        Path file = workingDir.resolve("test_public_key.pem");
        pubKey = Files.readString(file);
        file = workingDir.resolve("test_private_key.pem");
        priKey = Files.readString(file);
        assertNotNull(pubKey);
        assertNotNull(priKey);
    }

    @Test
    void verify() throws Exception {
        String message = "Hello World!";
        String signature = RsaSignature.sign(message, priKey);
        System.out.println(signature);
        assertTrue(RsaSignature.verify( message, signature, pubKey));
        assertFalse(RsaSignature.verify( message, "dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdA==", pubKey));

    }
}