package dev.fusionize.orchestrator.ai;

import dev.fusionize.ai.exception.AgentConfigDomainAlreadyExistsException;
import dev.fusionize.ai.exception.AgentConfigException;
import dev.fusionize.ai.exception.AgentConfigNotFoundException;
import dev.fusionize.ai.exception.InvalidAgentConfigException;
import dev.fusionize.common.exception.ExceptionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConfigControllerAdvisorTest {

    private final AgentConfigControllerAdvisor advisor = new AgentConfigControllerAdvisor();

    @Test
    void shouldReturnConflict_forDomainAlreadyExists() {
        // setup
        var ex = new AgentConfigDomainAlreadyExistsException("domain already exists");

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
        var ex = new InvalidAgentConfigException("invalid config");

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleBadRequest(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturnInternalServerError_forGenericException() {
        // setup
        var ex = new AgentConfigException("something went wrong");

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleGeneric(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(500);
    }

    @Test
    void shouldReturnNotFound_forNotFoundException() {
        // setup
        var ex = new AgentConfigNotFoundException("config not found");

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleNotFound(ex);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHttpStatus()).isEqualTo(404);
    }

    @Test
    void shouldIncludeExceptionMessageInResponse() {
        // setup
        var message = "agent config not found for domain test.agent";
        var ex = new AgentConfigNotFoundException(message);

        // expectation
        ResponseEntity<ExceptionResponse> response = advisor.handleNotFound(ex);

        // validation
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains(message);
    }
}
