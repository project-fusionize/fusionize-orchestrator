package dev.fusionize.orchestrator.ai;

import dev.fusionize.ai.exception.ChatModelConnectionException;
import dev.fusionize.ai.exception.ChatModelDomainAlreadyExistsException;
import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.ai.exception.ChatModelNotFoundException;
import dev.fusionize.ai.exception.InvalidChatModelConfigException;
import dev.fusionize.common.exception.ExceptionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModelControllerAdvisorTest {

    private final ChatModelControllerAdvisor advisor = new ChatModelControllerAdvisor();

    @Test
    void shouldReturnNotFound_forChatModelNotFound() {
        // setup
        var ex = new ChatModelNotFoundException("model not found");

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
        var ex = new ChatModelDomainAlreadyExistsException("domain already exists");

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleDomainExists(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(409);
    }

    @Test
    void shouldReturnBadRequest_forInvalidConfig() {
        // setup
        var ex = new InvalidChatModelConfigException("invalid config");

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
        var ex = new ChatModelConnectionException("connection failed", new RuntimeException("timeout"));

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
        var ex = new ChatModelException("unexpected error");

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleGeneric(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(500);
    }
}
