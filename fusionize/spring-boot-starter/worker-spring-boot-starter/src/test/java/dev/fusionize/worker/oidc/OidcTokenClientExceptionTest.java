package dev.fusionize.worker.oidc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OidcTokenClientExceptionTest {

    @Test
    void shouldCreateWithNoArgs() {
        // setup + expectation
        var exception = new OidcTokenClientException();

        // validation
        assertThat(exception).isInstanceOf(OidcTokenClientException.class);
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessage() {
        // setup
        var message = "(oidc101) test error message";

        // expectation
        var exception = new OidcTokenClientException(message);

        // validation
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        // setup
        var message = "(oidc102) something went wrong";
        var cause = new RuntimeException("root cause");

        // expectation
        var exception = new OidcTokenClientException(message, cause);

        // validation
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateWithCause() {
        // setup
        var cause = new IllegalStateException("underlying issue");

        // expectation
        var exception = new OidcTokenClientException(cause);

        // validation
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
