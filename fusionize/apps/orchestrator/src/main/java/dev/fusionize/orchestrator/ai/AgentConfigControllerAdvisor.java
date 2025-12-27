package dev.fusionize.orchestrator.ai;

import dev.fusionize.ai.exception.AgentConfigDomainAlreadyExistsException;
import dev.fusionize.ai.exception.AgentConfigException;
import dev.fusionize.ai.exception.InvalidAgentConfigException;
import dev.fusionize.ai.exception.AgentConfigNotFoundException;
import dev.fusionize.common.exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AgentConfigControllerAdvisor {

    @ExceptionHandler(AgentConfigDomainAlreadyExistsException.class)
    public ResponseEntity<ExceptionResponse> handleDomainExists(AgentConfigDomainAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidAgentConfigException.class)
    public ResponseEntity<ExceptionResponse> handleBadRequest(InvalidAgentConfigException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AgentConfigException.class)
    public ResponseEntity<ExceptionResponse> handleGeneric(AgentConfigException ex) {
        return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AgentConfigNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNotFound(AgentConfigNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<ExceptionResponse> buildResponse(Exception ex, HttpStatus status) {
        ExceptionResponse response = ExceptionResponse.builder(ex)
                .withHttpStatus(status.value())
                .build();
        return new ResponseEntity<>(response, status);
    }
}
