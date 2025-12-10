package dev.fusionize.orchestrator.ai;

import dev.fusionize.ai.exception.*;
import dev.fusionize.common.exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChatModelControllerAdvisor {

    @ExceptionHandler(ChatModelNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNotFound(ChatModelNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ChatModelDomainAlreadyExistsException.class)
    public ResponseEntity<ExceptionResponse> handleDomainExists(ChatModelDomainAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({ InvalidChatModelConfigException.class, UnsupportedChatModelProviderException.class })
    public ResponseEntity<ExceptionResponse> handleBadRequest(ChatModelException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ChatModelConnectionException.class)
    public ResponseEntity<ExceptionResponse> handleConnectionError(ChatModelConnectionException ex) {
        return buildResponse(ex, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(ChatModelException.class)
    public ResponseEntity<ExceptionResponse> handleGeneric(ChatModelException ex) {
        return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ExceptionResponse> buildResponse(Exception ex, HttpStatus status) {
        ExceptionResponse response = ExceptionResponse.builder(ex)
                .withHttpStatus(status.value())
                .build();
        return new ResponseEntity<>(response, status);
    }
}
