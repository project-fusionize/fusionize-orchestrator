package dev.fusionize.storage.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageExceptionTest {

    private static final String MESSAGE = "something went wrong";
    private static final Throwable CAUSE = new RuntimeException("root cause");

    // ---- StorageException ----

    @Test
    void storageException_message() {
        // setup
        var exception = new StorageException(MESSAGE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(cause).isNotNull();
        assertThat(cause.getMessage()).isEqualTo(MESSAGE);
    }

    @Test
    void storageException_messageAndCause() {
        // setup
        var exception = new StorageException(MESSAGE, CAUSE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(cause).isNotNull();
        assertThat(cause.getMessage()).isEqualTo(MESSAGE);
        assertThat(cause.getCause()).isEqualTo(CAUSE);
    }

    // ---- StorageNotFoundException ----

    @Test
    void storageNotFoundException_containsDomainInMessage() {
        // setup
        var domain = "my-domain";

        // expectation
        var exception = new StorageNotFoundException(domain);

        // validation
        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause().getMessage()).contains("my-domain");
        assertThat(exception.getCause().getMessage()).isEqualTo("Storage config not found for domain: my-domain");
    }

    // ---- StorageDomainAlreadyExistsException ----

    @Test
    void storageDomainAlreadyExistsException_containsDomainInMessage() {
        // setup
        var domain = "existing-domain";

        // expectation
        var exception = new StorageDomainAlreadyExistsException(domain);

        // validation
        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause().getMessage()).contains("existing-domain");
        assertThat(exception.getCause().getMessage()).isEqualTo("Storage config with domain existing-domain already exists");
    }

    // ---- StorageConnectionException ----

    @Test
    void storageConnectionException_messageAndCause() {
        // setup
        var message = "connection failed";
        var cause = new RuntimeException("timeout");

        // expectation
        var exception = new StorageConnectionException(message, cause);

        // validation
        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause().getMessage()).isEqualTo(message);
        assertThat(exception.getCause().getCause()).isEqualTo(cause);
    }
}
