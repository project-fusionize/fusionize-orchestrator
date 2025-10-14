package dev.fusionize.security.crypto.rsa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.*;

class RsaEncrypterTest {
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
    void encryptToString() throws Exception {
        String encrypted = RsaEncrypter.encryptToString("Hello World in Java!!!", pubKey);
        System.out.println(encrypted);
        String decrypted = RsaDecrypter.decryptToString(encrypted, priKey);
        assertEquals("Hello World in Java!!!", decrypted);
        System.out.println(decrypted);
    }
}