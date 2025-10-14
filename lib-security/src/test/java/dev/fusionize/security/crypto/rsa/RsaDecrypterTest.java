package dev.fusionize.security.crypto.rsa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RsaDecrypterTest {
    private String priKey;

    @BeforeEach
    public void init() throws IOException {
        Path workingDir = Path.of("", "src/test/resources");
        Path file = workingDir.resolve("test_private_key.pem");
        priKey = Files.readString(file);
        assertNotNull(priKey);
    }

    @Test
    void decryptToString() throws Exception {
        String encrypted = "Ntqwyr90S/qIM2WhoyOAfUzKGBZ1DnRC0pj4gvTabWndOJtcAs053w1cMp2Yng7rFgzTGn2VkSwG+v9kv8S8v7Oe/R3NWWzbbp4DZXyP0mZLBpz2500pI+uE2zHc20ew076H2O9wJzPhjScO+ntBYgtUHTNVa7cwxyeJbh9uaKbfqIKlKJC6LfRwLHE/UOdNVcJkZroIuvvsbyNjnhPWrAzlVW6Pe0XS3DAFa+lA2SD3fdQYE68AwFg4LHFPOtwBvjb7dTkk5AEq2rC/Egsfe67CwlGzqwqZrKGWkmi9Cb1lev1R8Ng5xQpLhrP8taA6xk9whVxy/9ZPguOoIW4sWg==";
        assertEquals("Hello World in Java!!!", RsaDecrypter.decryptToString(encrypted, priKey));
    }
}