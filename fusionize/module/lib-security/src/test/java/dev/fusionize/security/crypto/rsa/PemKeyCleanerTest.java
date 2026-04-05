package dev.fusionize.security.crypto.rsa;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PemKeyCleanerTest {

    private static final String VALID_BASE64 = Base64.getEncoder().encodeToString("test-key-content".getBytes());

    @Test
    void shouldCleanPrivateKeyPem() {
        // setup
        var pemKey = "-----BEGIN PRIVATE KEY-----\n" + VALID_BASE64 + "\n-----END PRIVATE KEY-----";

        // expectation
        var result = PemKeyCleaner.cleanPem(pemKey);

        // validation
        assertThat(result).isEqualTo(VALID_BASE64);
        assertThat(result).doesNotContain("-----BEGIN PRIVATE KEY-----");
        assertThat(result).doesNotContain("-----END PRIVATE KEY-----");
    }

    @Test
    void shouldCleanPublicKeyPem() {
        // setup
        var pemKey = "-----BEGIN PUBLIC KEY-----\n" + VALID_BASE64 + "\n-----END PUBLIC KEY-----";

        // expectation
        var result = PemKeyCleaner.cleanPem(pemKey);

        // validation
        assertThat(result).isEqualTo(VALID_BASE64);
        assertThat(result).doesNotContain("-----BEGIN PUBLIC KEY-----");
        assertThat(result).doesNotContain("-----END PUBLIC KEY-----");
    }

    @Test
    void shouldCleanRsaPrivateKeyPem() {
        // setup
        var pemKey = "-----BEGIN RSA PRIVATE KEY-----\n" + VALID_BASE64 + "\n-----END RSA PRIVATE KEY-----";

        // expectation
        var result = PemKeyCleaner.cleanPem(pemKey);

        // validation
        assertThat(result).isEqualTo(VALID_BASE64);
        assertThat(result).doesNotContain("-----BEGIN RSA PRIVATE KEY-----");
        assertThat(result).doesNotContain("-----END RSA PRIVATE KEY-----");
    }

    @Test
    void shouldRemoveNewlinesAndWhitespace() {
        // setup
        var pemKey = "-----BEGIN PRIVATE KEY-----\n"
                + VALID_BASE64.substring(0, 10) + "\n"
                + VALID_BASE64.substring(10) + "\n"
                + "-----END PRIVATE KEY-----";

        // expectation
        var result = PemKeyCleaner.cleanPem(pemKey);

        // validation
        assertThat(result).isEqualTo(VALID_BASE64);
        assertThat(result).doesNotContain("\n");
        assertThat(result).doesNotContain("\r");
    }

    @Test
    void shouldThrow_forNullInput() {
        // setup
        String pemKey = null;

        // expectation / validation
        assertThatThrownBy(() -> PemKeyCleaner.cleanPem(pemKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PEM key content cannot be null");
    }

    @Test
    void shouldThrow_forInvalidBase64() {
        // setup
        var pemKey = "not-a-key!!!";

        // expectation / validation
        assertThatThrownBy(() -> PemKeyCleaner.cleanPem(pemKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Key content is not valid Base64 after cleanup.");
    }
}
