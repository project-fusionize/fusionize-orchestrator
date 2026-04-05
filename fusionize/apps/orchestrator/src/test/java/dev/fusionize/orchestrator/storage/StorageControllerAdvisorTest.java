package dev.fusionize.orchestrator.storage;

import dev.fusionize.common.exception.ExceptionResponse;
import dev.fusionize.storage.exception.StorageConnectionException;
import dev.fusionize.storage.exception.StorageDomainAlreadyExistsException;
import dev.fusionize.storage.exception.StorageException;
import dev.fusionize.storage.exception.StorageNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class StorageControllerAdvisorTest {

    private final StorageControllerAdvisor advisor = new StorageControllerAdvisor();

    @Test
    void shouldReturnNotFound_forStorageNotFound() {
        // setup
        var ex = new StorageNotFoundException("test-domain");

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleNotFound(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(404);
    }

    @Test
    void shouldReturnConflict_forDomainAlreadyExists() {
        // setup
        var ex = new StorageDomainAlreadyExistsException("test-domain");

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleDomainExists(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(409);
    }

    @Test
    void shouldReturnBadRequest_forIllegalArgument() {
        // setup
        var ex = new IllegalArgumentException("invalid argument");

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleBadRequest(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturnBadGateway_forConnectionError() {
        // setup
        var ex = new StorageConnectionException("connection failed", new RuntimeException("timeout"));

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleConnectionError(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(502);
    }

    @Test
    void shouldReturnInternalServerError_forGenericException() {
        // setup
        var ex = new StorageException("unexpected error");

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleGeneric(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(500);
    }
}
